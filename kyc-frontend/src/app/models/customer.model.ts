export interface Customer {
    id?: string;
    fullName: string;
    nationality: string;
    residenceCountry: string;
    occupation?: string;
    industrySector?: string;
    incomeRange?: string;
    sourceOfWealth?: string;
    entityType?: string;
    netWorth?: number;
    accountAge?: number;
    expectedMonthlyVolume?: number;
    userId?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface CustomerFullProfile {
    customer: Customer;
    products: any[]; // Use specific Product type if available
    transactions: any[]; // Use specific Transaction type if available
}
