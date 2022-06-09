package com.example.agidospringv2.service

import com.example.agidospringv2.enum.XError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URLEncoder

@Component
class HTMLWrapper {


    @Autowired
    lateinit var interim: Interim

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var testDataService: TestDataService

    @Autowired
    lateinit var env: Environment


    fun loadTestData(): String {
        return testDataService.loadTestData()
            .joinToString("<br>") + "Für alle Test-Accounts ist das Passwort: test" + backButton()
    }

    fun registerCustomer(username: String, password: String): String {
        return "Kunde \"$username\" erfolgreich angelegt.<br>${
            userService.registerNewUser(
                username, password, UserType.Customer
            ).print()
        }" + backButton()
    }

    fun registerServiceEmployee(username: String, password: String, userType: UserType): String {
        return "Service-Mitarbeiter $username erfolgreich angelegt.<br>" + userService.registerNewUser(
            username, password, UserType.ServiceEmployee
        ).print() + backButton()
    }

    fun DTO.wrap(sRequests: SRequests): String {
        this.errorMsg?.let {
            return it + backButton()
        }
        var ret = ""
        ret = when (sRequests) {
            SRequests.Deposit, SRequests.Withdraw -> {
                this.msg ?: errorMsg ?: ""
            }
            SRequests.ShowPendingTransactions -> {
                this@wrap.transactionList.htmlWrapTransactionList(this.displayWithButtons)
            }
            SRequests.AuthorizeTransaction -> {
                this@wrap.msg ?: errorMsg ?: ""
            }
            SRequests.ShowSumOfAllTransactions -> {
                this@wrap.transactionList.showTransactionSumMeta()
                // showTransactionSumMeta
            }
            SRequests.ShowAllTransactions -> {
                this@wrap.transactionList.htmlWrapTransactionList()
            }
            SRequests.ShowRichest -> {
                "<table><tr><th>Name</th><th>Kontostand</th><th>UserId</th></tr>" + (this@wrap.userList?.let { list ->
                    list.joinToString(separator = "") { "<tr><td>${it.name}</td><td>${it.getBalance()}</td><td>${it.userId}</td></tr>" }
                } ?: "") + "</table>"
            }
            else -> this.msg ?: errorMsg ?: ""
        }
        return "${backButton()}$ret${sRequests}${backButton()}"
    }

    fun process(request: SRequests, user: User, additionalValues: AdditionalValues): String {

        return interim.process(
            user,
            request,
            amount = additionalValues.amount,
            transactionId = additionalValues.transactionId,
            from = additionalValues.fromstr,
            to = additionalValues.tostr
        ).wrap(request)

    }


    fun List<Transaction>?.htmlWrapTransactionList(withButtons:Boolean=false): String {
        return this?.let { list ->
            return "<table>" + if (list.isNotEmpty()) {
                list[0].htmlTableHeaders() + list.joinToString(separator = "") { it.htmlTableRowPrint() + if(withButtons){it.transactionButton()}else "" }
            } else {
                XError.NoTransactionsFound.send()
            } + "</table>"
        } ?: return XError.NoTransactionsFound.send()
    }

    fun Transaction.transactionButton():String
    {
        return "\"<td><form action=\"${env.getProperty("request.customer.authorize.withdrawal")!!}\" method=\"get\" target=\"_blank\">" + "<input type=\"text\" id=\"transactionId\" name=\"transactionId\" value=\"${this.transactionId}\"></input>" + "<input type=\"submit\" value=\"freigeben\">" + "</form></td>"
    }

    fun List<Transaction>?.showTransactionSumMeta(): String {
        var splittedList= this?.let { list-> list.groupBy { it.transactionType }}?:return XError.NoTransactionsFound.send()
        mutableListOf<TransactionType>(TransactionType.Deposit,TransactionType.Withdrawal)

        var html = "<table><tr><th>alle</th><th>nur Einzahlungen</th><th>nur Auszahlungen</th></tr>"
        html += "<tr><td>Gesamtsumme: ${this.filter { it.transactionType==TransactionType.Deposit||it.transactionType==TransactionType.Withdrawal }.sumOf {
            when(it.transactionType){
                TransactionType.Deposit->it.amount
                    TransactionType.Withdrawal->it.amount.multiply(-BigDecimal.ONE)
            }
             }}</td><td>Gesamtsumme: ${splittedList[TransactionType.Deposit]?.sumOf { it.amount }?:0}</td><td>Gesamtsumme: ${splittedList[TransactionType.Withdrawal]?.sumOf { it.amount }?:0}</td></tr>"
        html += "<tr><td>${this.htmlWrapTransactionList()}</td><td>${splittedList[TransactionType.Deposit].htmlWrapTransactionList()}</td><td>${splittedList[TransactionType.Withdrawal].htmlWrapTransactionList()}</td></tr></table>"
        return html

    }


    fun welcomePage(user: User?): String {

        var add = ""

        userService.identify(user)?.let {

            when (it.userType) {
                UserType.Customer -> {
                    var map = mutableMapOf<String, String>(

                        "Einzahlen" to env.getProperty("request.customer.deposit")!!,
                        "Auszahlen" to env.getProperty("request.customer.withdraw")!!
                    )
                    map.forEach { (k, v) ->
                        add += numberInput("/$v", k)
                    }
                    var map2 = mutableMapOf<String, String>(
                        "Transaktionen zeigen" to env.getProperty("request.all.show.transactions")!!,
                        "Transaktionssumme zeigen" to env.getProperty("request.all.show.transactionsum")!!
                    )

                    map2.forEach { (k, v) ->
                        add += fromToDisplay("/$v", k)
                    }

                    mutableMapOf<String, String>("auf Bestätigung wartende Transaktionen anzeigen." to env.getProperty("request.customer.show.pendingwithdrawals")!!).forEach { k, v ->
                        add += button("/$v", k)

                    }

                }
                UserType.ServiceEmployee -> {
                    mutableMapOf<String, String>().apply {

                    }


                    mutableMapOf<String, String>(
                        "Transaktionen zeigen" to env.getProperty("request.all.show.transactions")!!,
                        "Transaktionssumme zeigen" to env.getProperty("request.all.show.transactionsum")!!,
                    ).forEach { k, v ->
                        add += fromToDisplay("/$v", k)
                    }

                    mutableMapOf<String, String>("auf Bestätigung wartende Transaktionen anzeigen." to env.getProperty("request.serviceemployee.show.pendingwithdrawals")!!).forEach { k, v ->
                        add += button("/$v", k)

                    }
                    add+=button("/${env.getProperty("request.serviceemployee.show.richest")}","reichsten User anzeigen")

                }
                else -> {


                }
            }

        }


        var notLoggedin = "<br>Bitte zuerst als Admin mit Username:abc und Passwort:123 anmelden."
        var b = ""
        if (user?.username == "abc") {
            b =
                "<br><form action=\"${env.getProperty("request.admin.register.serviceemployee.url")}\" method=\"get\" target=\"_blank\">" + "Name<input type=\"text\" id=\"name\" name=\"name\">" + "Passwort<input type=\"text\" id=\"password\" name=\"password\">" + "<input type=\"submit\" value=\"Service-Mitarbeiter anlegen\">" + "</form><br><form action=\"${
                    env.getProperty("request.all.register.customer.url")
                }\" method=\"get\" target=\"_blank\">" + "Name<input type=\"text\" id=\"name\" name=\"name\">" + "Passwort<input type=\"text\" id=\"password\" name=\"password\">" + "<input type=\"submit\" value=\"Kunde anlegen\">" + "</form><br>" + button(
                    "/${env.getProperty("request.admin.load.testdata.url")!!}", "Testdaten laden"
                )
        }
        return "Agido-Testaufgabe<br>Willkommen ${user?.username ?: notLoggedin}<br>$add${b}"
    }

    fun fromToDisplay(url: String, infoText: String): String {
        return "<form action=\"${url}\" method=\"get\" target=\"_blank\">\n" + "  <label for=\"from\">von:</label>\n" + "  <input type=\"text\" id=\"from\" name=\"from\">\n" + "  <label for=\"to\">bis</label>\n" + "  <input type=\"text\" id=\"to\" name=\"to\">  <input type=\"submit\" value=\"$infoText\">\n" + "</form>"
    }

    fun numberInput(url: String, text: String): String {
        return "<form action=\"$url\" method=\"get\" target=\"_blank\">" + "<input type=\"text\" id=\"amount\" name=\"amount\">" + "<input type=\"submit\" value=\"$text\">\n" + "</form>"
    }

    fun button(url: String, text: String): String {
        return "<form action=\"${url}\" method=\"get\" target=\"_blank\">" + "<input type=\"submit\" value=\"$text\">" + "</form>"
    }

    fun backButton(): String {
        return button("/", "zurück")
    }

}