package com.example.agidospringv2.service

import com.example.agidospringv2.enum.XError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class Interim {

    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var userService: UserService


    var nex = mutableMapOf<UserType, MutableList<SRequests>>().apply {
        this[UserType.Customer] =
            mutableListOf(
                SRequests.Deposit,
                SRequests.ShowAllTransactions,
                //  SRequests.ShowTransactionSum,
                SRequests.Withdraw
            )
        this[UserType.ServiceEmployee] = mutableListOf(
            SRequests.ShowAllTransactions,
            SRequests.AuthorizeTransaction,
            //    SRequests.ShowTransactionSum,
            SRequests.ShowRichest
        )

        SRequests.AuthorizeTransaction
    }

    fun process(user: User, sRequests: SRequests, additionalValues: AdditionalValues) {

    }

    fun String.toErrorDTO(): DTO {
        return DTO().apply { errorMsg = this@toErrorDTO }
    }

    fun process(
        user: User,
        sRequests: SRequests,
        from: String? = null,
        to: String? = null,
        targetUserId: String? = null,
        amount: BigDecimal? = null,
        transactionId: String? = null
    ): DTO {
        println("processx")
        userService.identify(user)?.let { appUser ->
            println("uentered")
            println(appUser.userType)
            println(nex[appUser.userType])
            println(nex[appUser.userType]?.contains(sRequests))
            println("debug over")
            nex[appUser.userType]?.contains(sRequests) ?: return XError.NoRights.toDTO()
            AdditionalValues().let { tc ->
                tc.userService=userService
                tc.transactionService=transactionService
                println("Time")
                tc.checkTimeErrors(from, to)?.let { return it.toErrorDTO() }
                println("TargetUserId: $targetUserId")
                tc.checkTargetUserErrors(targetUserId)?.let { return it.toErrorDTO() }
                println("Transaction")
                tc.checkTransactionErrors(transactionId)?.let { return it.toErrorDTO() }
                println("amount")
                tc.checkAmountErrors(amount)
                println("perform")
                return sRequests.perform(this@Interim,appUser, tc).apply { displayWithButtons=(appUser.userType==UserType.ServiceEmployee) }
            }
        } ?: return XError.UserNotFound.toDTO()
    }

    fun deposit(appUser: AppUser, additionalValues: AdditionalValues): DTO {
        transactionService.deposit(
            appUser,
            additionalValues.amount ?: return DTO().apply { errorMsg = XError.NoAmount.msg }).let {
            return DTO().apply {
                msg = it
            }
        }
    }

    fun requestWithdrawal(appUser: AppUser, additionalValues: AdditionalValues): DTO {
        transactionService.requestWithdrawal(
            appUser,
            additionalValues.amount ?: return DTO().apply { errorMsg = XError.NoAmount.msg }).let {
            return DTO().apply {
                msg = it
            }
        }

    }

    fun getTransactions(appUser: AppUser, additionalValues: AdditionalValues): DTO {
        if(appUser.userType==UserType.Customer && additionalValues.targetUser==null){
            additionalValues.targetUser=appUser
        }
        if (appUser.userType == UserType.Customer && additionalValues.targetUser != appUser) return DTO().apply {
            errorMsg = XError.CustomerCanOnlySeeOwnTransactions.msg
        }
        if (appUser.userType == UserType.Customer) {
            return DTO().apply {
                transactionList = transactionService.getTransactions(appUser.userId, additionalValues)
            }
        }
        if (appUser.userType != UserType.ServiceEmployee) return DTO().apply {
            errorMsg = XError.NoRights.send(appUser.userType.name)
        }
        //=ServiceEmployee
        if (appUser == additionalValues.targetUser) return DTO().apply {
            errorMsg = XError.ServiceEmployeeDoesntHaveTransactions.msg
        }
        return DTO().apply {
            transactionList = transactionService.getTransactions(additionalValues.targetUser?.userId, additionalValues)
        }
    }

    fun showRichest(appUser: AppUser): DTO {

        println("entered")
        return DTO().apply {

            userList = userService.sortAllCustomersByAmountOfMoney(appUser)
            println(userList?.joinToString { it.print() })
        }

    }

    fun authorizeTransaction(appUser: AppUser, additionalValues: AdditionalValues): DTO {

        println(additionalValues.transactionId)
        println(additionalValues.targetUser?.userId?:"NO USAr")
        transactionService.authorizeWithdrawal(additionalValues.transactionId!!)
        return DTO()
    }


}


class AdditionalValues() {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var transactionService: TransactionService

    private val zonedDateTimeAdd = "T00:00:00+01:00"
    private fun String.YYYYMMDDtoParsableZonedDateTime(): ZonedDateTime? {
        return ZonedDateTime.parse(this + zonedDateTimeAdd, DateTimeFormatter.ISO_ZONED_DATE_TIME) ?: null
    }

    var fromstr: String? = null
    var tostr: String? = null
    var from: ZonedDateTime? = null
    var to: ZonedDateTime? = null
    var amount: BigDecimal? = null
    var targetUserId: String? = null
    var targetUser: AppUser? = null
    var filterByTransactionType: TransactionType? = null
    var filterByTransactionStatus: TransactionStatus? = null
    var transactionId: String? = null

    fun checkTimeErrors(): String? {
        return checkTimeErrors(fromstr, tostr)
    }

    fun checkTargetUserErrors(): String? {
        return checkTargetUserErrors(targetUserId)
    }

    fun checkTransactionErrors(): String? {
        return checkTransactionErrors(transactionId)
    }


    fun checkTimeErrors(fromx: String?, tox: String?): String? {
        from?:to?:return null
        to?:from?:return null
        fromx?.YYYYMMDDtoParsableZonedDateTime()?.let {
            from = it
        } ?: return XError.WrongDateFormat.send(fromx?:"null")
        tox?.YYYYMMDDtoParsableZonedDateTime()?.let {
            to = it
        } ?: return XError.WrongDateFormat.send(tox?:"null")

        return null

    }

    fun checkTargetUserErrors(id: String?): String? {
        id ?: return null
        targetUser = userService.identify(id) ?: return XError.UserNotFound.msg
        return null
    }

    fun checkTransactionErrors(id: String?): String? {
        id ?: return null
        transactionService.getTransactionById(id) ?: return XError.TransactionNotFound.send(id)
        transactionId = id
        return null

    }
    fun checkAmountErrors(amountx:BigDecimal?): String?
    {
        amountx?: return null
        amount=amountx
        return null
    }

}


class DTO() {
    var msg: String? = null
    var errorMsg: String? = null
    var transactionList: List<Transaction>? = null
    var userList: List<AppUser>? = null
    var displayWithButtons=false
    fun extract(): String {
        return ""
    }
}


enum class SRequests(val perform: (Interim, AppUser, AdditionalValues) -> DTO, val checks: (AdditionalValues) -> String?) {


    Deposit({ interim,user, av -> interim.deposit(user, av) }, { av -> null }),
    Withdraw({interim, user, av -> interim.requestWithdrawal(user, av) }, { av -> null }),
    ShowAllTransactions({ interim, user, av -> interim.getTransactions(user, av) }, { av -> null }),
    ShowPendingTransactions({ interim, user, av ->
        interim.getTransactions(user, av.apply {
            filterByTransactionType = TransactionType.Withdrawal
            filterByTransactionStatus = TransactionStatus.PermissionPending

        })
    }, { av -> null }),
    ShowRichest({ interim, user, av  -> interim.showRichest(user) }, { av -> null }),
    AuthorizeTransaction({ interim, user, av  -> interim.authorizeTransaction(user, av) }, { av -> null }),
    ShowSumOfAllTransactions({interim, user, av -> interim.getTransactions(user, av) }, { av -> null })
    ;
}
