package com.example.agidospringv2.enum

import com.example.agidospringv2.service.DTO


enum class XError(val msg: String) {
    UserNotFound("User nicht gefunden."),
    NoRights("Erforderlichen Rechte nicht vorhanden."),
    WrongDateFormat("YYYY-MM-DD Muster gefordert."),
    NoAmount("Kein Betrag angegeben."),
    CustomerCanOnlySeeOwnTransactions("Kunden können nur ihre eigenen Transaktionen sehen."),
    ServiceEmployeeDoesntHaveTransactions("Servicemitarbeiter haben keine Transaktionen."),
    TransactionNotFound("Transaktion wurde nicht gefunden."),
    NoTransactionsFound("Keine ausstehende Transaktionen gefunden"),
    InsufficientFunds("Konto nicht ausreichend gedeckt."),
    UnknownError("Etwas hat nicht funktioniert...")
    ;

    fun send():String= msg
    fun send(input: String): String {
        return "$msg<br>Ihr eingegebener Wert: $input"
    }
    fun toDTO():DTO
    {return DTO().apply { errorMsg=msg }}
    fun toDTO(input:String):DTO
    {
        return DTO().apply { errorMsg=send(input) }
    }

}