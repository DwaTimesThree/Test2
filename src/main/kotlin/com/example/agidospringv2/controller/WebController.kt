package com.example.agidospringv2.controller

import com.example.agidospringv2.enum.XError
import com.example.agidospringv2.service.AdditionalValues
import com.example.agidospringv2.service.HTMLWrapper
import com.example.agidospringv2.service.SRequests
import com.example.agidospringv2.service.UserType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
class WebController {

    @Autowired
    lateinit var htmlWrapper: HTMLWrapper

    @RequestMapping("\${request.all.register.customer.url}")
    fun register(@RequestParam("name") username: String, @RequestParam("password") password: String): String {

        return htmlWrapper.registerCustomer(username, password)
    }


    @RequestMapping("\${request.all.show.transactions}")
    fun showTransactionsFromTo(
        @AuthenticationPrincipal user: User,
        @RequestParam("userId", required = false) userId: String?,
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?
    ): String {
        // return htmlWrapper.showTransactionsMeta(user, userId, from, to)
        return htmlWrapper.process(SRequests.ShowAllTransactions,user,AdditionalValues().apply {
            targetUserId=userId
        })
    }

    @RequestMapping()
    fun index(@AuthenticationPrincipal user: User?): String {
        return htmlWrapper.welcomePage(user)

    }

    @RequestMapping("\${request.all.show.transactionsum}")
    fun showTransactionSumOf(
        @AuthenticationPrincipal user: User,
        @RequestParam("userId", required = false) userId: String?,
        @RequestParam("from", required = false) from: String?, @RequestParam("to", required = false) to: String?
    ): String {
        return htmlWrapper.process(SRequests.ShowSumOfAllTransactions,user,AdditionalValues().apply {
            targetUserId=userId
            fromstr=from
            tostr=to
        })
    }


    @RequestMapping("\${request.admin.load.testdata.url}")
    fun loadTestData(): String {
        return htmlWrapper.loadTestData()
    }

    @RequestMapping("\${request.admin.register.serviceemployee.url}")
    fun registerServiceEmployee(
        @AuthenticationPrincipal user: User,
        @RequestParam("name") username: String,
        @RequestParam("password") password: String
    ): String {
        user.authorities.find { it.authority == "ROLE_ADMIN" }
            ?: return XError.NoRights.send() + user.authorities.joinToString { it.authority + "<br>" }
        return htmlWrapper.registerServiceEmployee(username, password, UserType.ServiceEmployee)
    }


    @RequestMapping("\${request.customer.deposit}")
    fun deposit(
        @AuthenticationPrincipal user: User,
        @RequestParam("amount") amountx: BigDecimal
    ): String {
       return  htmlWrapper.process(SRequests.Deposit,user,AdditionalValues().apply { amount=amountx })
    }

    @RequestMapping("\${request.customer.withdraw}")
    fun requestWithdrawal(
        @AuthenticationPrincipal user: User,
        @RequestParam("amount") amountx: BigDecimal
    ): String {
        return htmlWrapper.process(SRequests.Withdraw,user,AdditionalValues().apply { amount=amountx })
    }

    @RequestMapping("\${request.customer.show.pendingwithdrawals}")
    fun showMyPendingWithdrawals(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.process(SRequests.ShowPendingTransactions,user, AdditionalValues())
    }

    @RequestMapping("\${request.serviceemployee.show.pendingwithdrawals}")
    fun showPendingWithdrawals(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.process(SRequests.ShowPendingTransactions,user, AdditionalValues())

    }

    @RequestMapping("\${request.customer.authorize.withdrawal}")
    fun authorizeWithdrawal(
        @AuthenticationPrincipal user: User,
        @RequestParam("transactionId") transactionIdx: String
    )
            : String {
        return htmlWrapper.process(SRequests.AuthorizeTransaction,user,AdditionalValues().apply { transactionId=transactionIdx })
    }

    @RequestMapping("\${request.serviceemployee.show.richest}")
    fun showRichestCustomers(@AuthenticationPrincipal user: User): String {
        return htmlWrapper.process(SRequests.ShowRichest,user,AdditionalValues())

    }


}
