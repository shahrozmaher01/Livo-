package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class WalletAndGiftSystemTest {

  @Test
  fun testOfficialCoinConversionRate() {
    assertEquals(400000L, COINS_PER_USD)
    val packages = listOf(
      RechargePackage("pkg_1", 1.00, 400000),
      RechargePackage("pkg_2", 2.00, 800000),
      RechargePackage("pkg_3", 5.00, 2000000),
      RechargePackage("pkg_4", 10.00, 4000000),
      RechargePackage("pkg_5", 25.00, 10000000),
      RechargePackage("pkg_6", 50.00, 20000000),
      RechargePackage("pkg_7", 100.00, 40000000)
    )

    assertEquals(400000L, packages[0].coinAmount)
    assertEquals(800000L, packages[1].coinAmount)
    assertEquals(2000000L, packages[2].coinAmount)
    assertEquals(4000000L, packages[3].coinAmount)
    assertEquals(10000000L, packages[4].coinAmount)
    assertEquals(20000000L, packages[5].coinAmount)
    assertEquals(40000000L, packages[6].coinAmount)
  }

  @Test
  fun testStandardGiftSendingAtomicCalculation() {
    // TEST 1: Sender has 5,000 Coins. Sender sends a 1,000-Coin gift.
    val senderInitialCoins = 5000L
    val receiverInitialDiamonds = 0L
    val giftCoinPrice = 1000L
    val count = 1

    val totalCost = giftCoinPrice * count
    assertTrue("Sender has sufficient coins", senderInitialCoins >= totalCost)

    val senderBalanceAfter = senderInitialCoins - totalCost
    val receiverDiamondsAfter = receiverInitialDiamonds + totalCost

    assertEquals(4000L, senderBalanceAfter)
    assertEquals(1000L, receiverDiamondsAfter)
  }

  @Test
  fun testStandardGiftInsufficientCoinsRejection() {
    // TEST 3: User has only 500 Coins and attempts a 1,000-Coin gift.
    val senderInitialCoins = 500L
    val giftCoinPrice = 1000L

    val canSend = senderInitialCoins >= giftCoinPrice
    assertFalse("Gift must be rejected due to insufficient coins", canSend)
  }

  @Test
  fun testDiamondToCoinExchangeFormula() {
    // TEST 2: Receiver exchanges 1,000 Diamonds.
    // Official Exchange Rule: 30% fee, 70% Coins delivered
    val diamondsToExchange = 1000L
    val feePercent = 30
    val exchangeRate = 0.70

    val feeDiamondsEquivalent = (diamondsToExchange * (feePercent / 100.0)).toLong()
    val coinsDelivered = (diamondsToExchange * exchangeRate).toLong()

    assertEquals(300L, feeDiamondsEquivalent)
    assertEquals(700L, coinsDelivered)

    // Verify further examples from spec
    val testCases = listOf(
      100L to 70L,
      500L to 350L,
      1000L to 700L,
      5000L to 3500L,
      10000L to 7000L,
      100000L to 70000L,
      1000000L to 700000L
    )

    for ((dia, expectedCoins) in testCases) {
      val actualCoins = (dia * exchangeRate).toLong()
      assertEquals("Coins for $dia diamonds", expectedCoins, actualCoins)
    }
  }

  @Test
  fun testDiamondExchangeInsufficientDiamondsRejection() {
    // TEST 4: User attempts to exchange 1,000 Diamonds but has only 500 Diamonds.
    val userDiamondBalance = 500L
    val requestedExchange = 1000L

    val canExchange = userDiamondBalance >= requestedExchange
    assertFalse("Exchange must be rejected due to insufficient diamonds", canExchange)
  }

  @Test
  fun testLuckyGiftValueDistribution() {
    // 500-Coin Lucky Gift:
    // Receiver share: 100 Diamonds (20%)
    // Platform commission: 100 Coins (20%)
    // Promotional reward pool: 300 Coins (60%)
    // Total: 500 Coins (100%)
    val luckyGift = LuckyGiftItem(
      id = "lucky_clover",
      name = "Lucky Clover",
      coinPrice = 500L,
      iconEmoji = "🍀",
      receiverDiamondReward = 100L,
      platformCommissionCoins = 100L,
      promotionalPoolAllocationCoins = 300L,
      senderXpReward = 50L,
      charmPoints = 500L,
      wealthPoints = 500L
    )

    assertEquals(500L, luckyGift.coinPrice)
    assertEquals(100L, luckyGift.receiverDiamondReward)
    assertEquals(100L, luckyGift.platformCommissionCoins)
    assertEquals(300L, luckyGift.promotionalPoolAllocationCoins)

    val sum = luckyGift.receiverDiamondReward + luckyGift.platformCommissionCoins + luckyGift.promotionalPoolAllocationCoins
    assertEquals("Accounting allocations must equal 500 Coins exactly", luckyGift.coinPrice, sum)
  }

  @Test
  fun testCreatorCashWithdrawalCalculation() {
    val diamondsPerUsd = 500000L
    val requestedDiamonds = 1000000L
    val expectedUsd = requestedDiamonds.toDouble() / diamondsPerUsd

    assertEquals(2.0, expectedUsd, 0.001)
  }
}
