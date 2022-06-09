package com.example.agidospringv2.enum

import com.example.agidospringv2.service.Transaction
import java.util.StringJoiner

enum class SuccessMessage(val msg:String) {
    Deposit("Einzahlung erfolgreich."),
    WithdrawalRequest("Auszahlung beantragt."),
    ;

    fun send(transaction:Transaction):String
    {
        return "$msg<br>${transaction.transactionType.name} : ${transaction.amount} : ${transaction.status}"
    }
}