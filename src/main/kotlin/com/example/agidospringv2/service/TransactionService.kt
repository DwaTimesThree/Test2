package com.example.agidospringv2.service

import com.example.agidospringv2.enum.DBType
import com.example.agidospringv2.enum.SuccessMessage
import com.example.agidospringv2.enum.XError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZonedDateTime

@Component
class TransactionService {

    private var transactions = mutableListOf<Transaction>()

    @Autowired
    lateinit var externalServices: ExternalServices

    fun getTransactionById(id: String?): Transaction? {
        id ?: return null
        return transactions.find { it.transactionId == id }
    }

    private fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }

    fun getTransactions(appUserId: String?, additionalValues: AdditionalValues): List<Transaction> {
        return transactions.filter { transaction ->
            transaction.fromToUserFilter(
                appUserId,
                additionalValues.from,
                additionalValues.to
            ) && transaction.typeFilter(additionalValues.filterByTransactionType) && transaction.statusFilter(
                additionalValues.filterByTransactionStatus
            )
        }
    }

    fun Transaction.writeTransactionIntoDB(dbType: DBType) {
        //         Aufgabenstellung: Speicherung in Memory
        when (dbType) {
            DBType.Memory -> {

                transactions.find { (it.transactionType == TransactionType.Deposit || it.transactionType == TransactionType.Withdrawal) && this.time == it.time }
                    ?.let {
                        this.time = ZonedDateTime.now()
                        this.writeTransactionIntoDB(dbType)
                        return

                    }
                addTransaction(this)
            }
            else -> {}

        }


    }


    fun authorizeWithdrawal( id: String) {
        transactions.first {
            !it.permissionGranted && (it.transactionId == id) // && transactionUser.isSameUserAs(it.actor)?:false
        }.apply {
            permissionGranted = true
        }.let { transaction ->
            externalServices.sendMoneyToCustomerAccount(transaction).let { response ->
                transaction.actor.addBalance(-transaction.amount)
                transaction.apply {
                    status = if (response.successful) TransactionStatus.Finished else TransactionStatus.Failed
                }
            }

        }

    }


    fun requestWithdrawal(appUser: AppUser, amount: BigDecimal): String {
        Transaction(TransactionType.Withdrawal, amount, appUser).apply {
            status =
                if (appUser.getBalance() < amount) TransactionStatus.Failed else TransactionStatus.PermissionPending
        }.let {
            it.writeTransactionIntoDB(DBType.Memory)
            when(it.status)
            {
                TransactionStatus.PermissionPending->{return SuccessMessage.WithdrawalRequest.send(it)}
                    TransactionStatus.Failed->{return XError.InsufficientFunds.send(it.amount.toString())
                    }
                else -> {return XError.UnknownError.msg}
            }

        }
    }


    fun MutableList<Transaction>.checkValidityOfWithdrawalRequests(): MutableList<Transaction> {
        this.forEach {
            if (it.amount > it.actor.getBalance()) {
                it.apply { status = TransactionStatus.Failed }
                return@forEach
            }
        }
        return this
    }



    fun deposit(user: AppUser, amount: BigDecimal): String {
        Transaction(TransactionType.Deposit, amount, user).apply {
            status =
                if (externalServices.startExternalDepositProcess(user).successful) TransactionStatus.Finished else TransactionStatus.Failed
        }.let {
            it.writeTransactionIntoDB(DBType.Memory)
            user.calcBalance(transactions)
            return SuccessMessage.Deposit.send(it)
        }
    }


    fun Transaction.typeFilter(tt: TransactionType?): Boolean {
        tt ?: return true
        return transactionType == tt
    }

    fun Transaction.statusFilter(ts: TransactionStatus?): Boolean {
        ts ?: return true
        return status == ts
    }

    fun Transaction.fromToUserFilter(
        userId: String? = null,
        from: ZonedDateTime? = null,
        to: ZonedDateTime? = null
    ): Boolean {
        userId ?: from ?: to?.let { return time <= to } ?: return true
        userId ?: to ?: from?.let { return time >= from }
        userId ?: return time <= to && time >= from
        from ?: to?.let { return time <= to && userId == actor.userId } ?: return userId == actor.userId
        to ?: from?.let { return time >= from && userId == actor.userId } ?: return userId == actor.userId
        return time >= from && userId == actor.userId && time <= to
    }

}