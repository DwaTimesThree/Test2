package com.example.agidospringv2.service

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class ExternalServices {

    fun startExternalDepositProcess(appUser: AppUser): DepositResponse {
        return DepositResponse()
    }

    fun sendMoneyToCustomerAccount(transaction:Transaction): WithdrawalResponse {
        return WithdrawalResponse()
    }
}

class DepositResponse {
    var successful = true
}

class WithdrawalResponse {
    var successful = true
}
