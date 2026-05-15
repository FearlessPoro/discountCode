package org.example.discountcode.coupon.api;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.example.discountcode.coupon.api.request.RedeemCouponRequest;
import org.example.discountcode.coupon.api.response.RedeemCouponResponse;
import org.example.discountcode.coupon.application.CouponRedemptionService;
import org.example.discountcode.coupon.application.CouponService;
import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.api.response.CouponResponse;
import org.example.discountcode.web.ClientIpResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponRedemptionService couponRedemptionService;
    private final ClientIpResolver clientIpResolver;

    public CouponController(
            CouponService couponService,
            CouponRedemptionService couponRedemptionService,
            ClientIpResolver clientIpResolver
    ) {
        this.couponService = couponService;
        this.couponRedemptionService = couponRedemptionService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.createCoupon(request);
    }

    @GetMapping("/{code}")
    CouponResponse getCoupon(@PathVariable String code) {
        return couponService.getCoupon(code);
    }

    @PostMapping("/{code}/redeem")
    RedeemCouponResponse redeemCoupon(
            @PathVariable String code,
            @Valid @RequestBody RedeemCouponRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        return couponRedemptionService.redeemCoupon(code, request.userId(), clientIp);
    }
}
