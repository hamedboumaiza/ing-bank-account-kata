package fr.ing.interview.bankaccountkata.service.impl;

import fr.ing.interview.bankaccountkata.exception.MoneyDepositNotPossibleException;
import fr.ing.interview.bankaccountkata.exception.MoneyWithdrawalNotPossibleException;
import fr.ing.interview.bankaccountkata.model.dto.AccountDTO;
import fr.ing.interview.bankaccountkata.model.dto.ClientDTO;
import fr.ing.interview.bankaccountkata.model.dto.TransactionDTO;
import fr.ing.interview.bankaccountkata.model.enums.TransactionTypeEnum;
import fr.ing.interview.bankaccountkata.service.AccountService;
import fr.ing.interview.bankaccountkata.service.ClientService;
import fr.ing.interview.bankaccountkata.service.TransactionService;
import fr.ing.interview.bankaccountkata.service.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final ClientService clientService;
    private final AccountService accountService;
    private final ClientMapper clientMapper;

    @Autowired
    public TransactionServiceImpl(ClientService clientService, AccountService accountService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.clientMapper = clientMapper;
    }

    @Override
    public List<TransactionDTO> getAllClientTransactionsByClientAndByAccountNumber(String clientId, String accountNumber) {
        ClientDTO clientDTO = clientMapper.toClientDTO(clientService.getClientIfExistThenThrow(clientId));
        AccountDTO accountDTO = accountService.findByAccountNumberFromAccountListThenThrow(clientDTO.getAccounts(), accountNumber);
        return accountDTO.getTransactions();
    }

    @Override
    public AccountDTO addTransaction(String clientId, String accountNumber, TransactionDTO transactionDTO) {
        ClientDTO clientDTO = clientMapper.toClientDTO(clientService.getClientIfExistThenThrow(clientId));
        AccountDTO accountDTO = accountService.findByAccountNumberFromAccountListThenThrow(clientDTO.getAccounts(), accountNumber);

        return treatTransaction(clientDTO, accountDTO, transactionDTO);
    }

    public AccountDTO treatTransaction(ClientDTO clientDTO, AccountDTO accountDTO, TransactionDTO transactionDTO) {
        DecimalFormat df = new DecimalFormat("0.00");

        transactionDTO.setTimestamp(LocalDateTime.now());
        if (transactionDTO.getTransactionType() == TransactionTypeEnum.DEPOSIT) {
            if (transactionDTO.getValue() > 0.01) {
                accountDTO.setBalance(Double.parseDouble(df.format(accountDTO.getBalance() + transactionDTO.getValue())));
            } else {
                throw new MoneyDepositNotPossibleException();
            }
        }

        if (transactionDTO.getTransactionType() == TransactionTypeEnum.WITHDRAWAL) {
            if (accountDTO.getBalance() - transactionDTO.getValue() > 0) {
                accountDTO.setBalance(Double.parseDouble(df.format(accountDTO.getBalance() - transactionDTO.getValue())));
            } else {
                throw new MoneyWithdrawalNotPossibleException();
            }
        }

        accountDTO.getTransactions().add(transactionDTO);
        clientDTO.getAccounts().set(clientDTO.getAccounts().indexOf(accountDTO), accountDTO);
        clientService.update(clientDTO);
        return accountDTO;
    }
}