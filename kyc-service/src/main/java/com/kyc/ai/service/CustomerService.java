package com.kyc.ai.service;

import com.kyc.ai.entity.Customer;
import com.kyc.ai.entity.FinancialTransaction;
import com.kyc.ai.entity.Product;
import com.kyc.ai.repository.CustomerRepository;
import com.kyc.ai.repository.FinancialTransactionRepository;
import com.kyc.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final FinancialTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creating new customer: {}", customer.getFullName());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(String id, Customer customerDetails) {
        Customer customer = getCustomerById(id);

        customer.setFullName(customerDetails.getFullName());
        customer.setNationality(customerDetails.getNationality());
        customer.setResidenceCountry(customerDetails.getResidenceCountry());
        customer.setOccupation(customerDetails.getOccupation());
        customer.setIndustrySector(customerDetails.getIndustrySector());
        customer.setIncomeRange(customerDetails.getIncomeRange());
        customer.setSourceOfWealth(customerDetails.getSourceOfWealth());
        customer.setEntityType(customerDetails.getEntityType());
        customer.setNetWorth(customerDetails.getNetWorth());
        customer.setAccountAge(customerDetails.getAccountAge());
        customer.setExpectedMonthlyVolume(customerDetails.getExpectedMonthlyVolume());

        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerFullProfile getCustomerFullProfile(String id) {
        Customer customer = getCustomerById(id);
        List<Product> products = productRepository.findByCustomerId(id);
        List<FinancialTransaction> transactions = transactionRepository.findByCustomerId(id);

        return new CustomerFullProfile(customer, products, transactions);
    }

    @Transactional
    public void deleteCustomer(String id) {
        customerRepository.deleteById(id);
    }

    public record CustomerFullProfile(
            Customer customer,
            List<Product> products,
            List<FinancialTransaction> transactions) {
    }
}
