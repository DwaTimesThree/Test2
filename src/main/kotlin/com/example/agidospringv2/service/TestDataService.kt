package com.example.agidospringv2.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Component
class TestDataService {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var transactionService: TransactionService

    var loaded = false
    fun loadTestData(): MutableList<String> {
        var nameList = mutableListOf<String>("Albert", "Bettina", "Carolin", "David", "Emma", "Friedrich", "Gertrud", "Hildegard",
                "Ivan", "Jessica", "Lars", "Monika")
        if (loaded) return nameList
        nameList.forEach {
            userService.registerNewUser(it, "test", UserType.Customer)
        }
        var amount = BigDecimal.ONE
        userService.userList.forEach { user ->
            user.name.forEach {

                transactionService.deposit(user, amount)
                amount += BigDecimal.ONE// BigDecimal(3)
            }
        }

        var serviceUser = AppUser().apply {
            name = "Norbert"
            userType = UserType.ServiceEmployee
        }
        with(transactionService) {
            requestWithdrawal(userService.userList[2], BigDecimal(200.0))
            requestWithdrawal(userService.userList[2], BigDecimal(300.0))
            requestWithdrawal(userService.userList[2], BigDecimal(2200.0))
            requestWithdrawal(userService.userList[1], BigDecimal(2200.0))
            requestWithdrawal(userService.userList[1], BigDecimal(10.0))
        }
        loaded = true
        return nameList
    }
}