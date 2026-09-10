package com.edtech.platform.catalog.facade;

import com.edtech.platform.catalog.facade.dto.PricingPackageSnapshot;

import java.util.UUID;

public interface PricingPackageFacade {
    PricingPackageSnapshot getPurchasablePackage(UUID pricingPackageId);
    PricingPackageSnapshot getPackageForPaymentFulfillment(UUID pricingPackageId);
}
