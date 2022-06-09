package com.example.agidospringv2.service

import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime

class AppUser() {

    var userId= ""
    var name = "###Username###"
    var userType = UserType.NonAssigned
    private var balance = BigDecimal.ZERO
    private var balanceLastUpdated = ZonedDateTime.now()
    var specialStatus = SpecialStatus.None

fun print():String
{
    var ret="<table>"

    mutableMapOf<String,String>().apply {
        this["id"]= userId.toString()
        this["name"]= name
        this["type"]= userType.name
        this["special status"] = specialStatus.name
    }.forEach{k,v->
        ret+="<tr><td>$k</td><td>$v</tr>"
    }

    return "$ret</table>"
}

    fun addBalance(amount:BigDecimal)
    {
        balance+=amount
        balanceLastUpdated = ZonedDateTime.now()
    }


    fun getBalance(): BigDecimal {
        return balance
    }

    fun getLastUpdateTime(): ZonedDateTime {
        return balanceLastUpdated
    }

    fun isSameUserAs(user: AppUser?):Boolean?
    {
        user?:return null
        return (this.userId==user.userId) &&(name==user.name)
    }

    fun calcBalance(transactions: MutableList<Transaction>) {
        var bal = BigDecimal.ZERO
        transactions.filter { it.actor.userId == this.userId }.forEach {

            if (it.status != TransactionStatus.Finished) return@forEach
            if (it.transactionType == TransactionType.Deposit) bal += it.amount
            if (it.transactionType == TransactionType.Withdrawal) bal -= it.amount
        }
        this.addBalance(bal)
    }



}

enum class UserType {
    NonAssigned,
    ServiceEmployee,
    Customer
}


enum class SpecialStatus{
    None,
    Banned
}