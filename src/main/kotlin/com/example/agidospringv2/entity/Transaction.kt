package com.example.agidospringv2.service
import com.example.agidospringv2.Sha
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

class Transaction(val transactionType: TransactionType, val amount: BigDecimal, val actor: AppUser) {
    var transactionId= createTransactionId()
    var time = ZonedDateTime.now()
    var permissionGranted = transactionType == TransactionType.Deposit
    var status = TransactionStatus.InProgress


    fun htmlTableHeaders():String{
        return "<tr><th>TransaktionsID</th><th>Art der Transaktion</th><th>Username</th><th>Betrag</th><th>Transaktionsstatus</th><th>Startzeitpunkt</th></tr>"
    }

    fun htmlTableRowPrint():String{
        return "<tr><td>$transactionId</td><td>${transactionType.name}</td><td>${actor.name}</td><td>$amount</td><td>$status</td><td>$time</td></tr>"
    }

    fun createTransactionId():String
    {
       var salt = "TNT4U#"
        return  Sha().calculateSH256("$salt#${actor.userId}#$amount#${actor.name}#${ZonedDateTime.now()}").encodeToByteArray().joinToString("")

    }


}

enum class TransactionType() {
    Withdrawal,
    Deposit
}

enum class TransactionStatus(){
    InProgress,
    PermissionPending,
    Finished,
    Failed
}