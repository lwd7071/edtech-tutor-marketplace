package com.edtech.platform.finance.util;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class AccountNumberCipherTest {
 @Test void encryptUsesRandomIvAndRoundTrips(){String raw="123456789012";String a=AccountNumberCipher.encrypt(raw),b=AccountNumberCipher.encrypt(raw);assertNotEquals(a,b);assertEquals(raw,AccountNumberCipher.decrypt(a));assertFalse(a.contains(raw));}
 @Test void tamperingIsRejected(){String encrypted=AccountNumberCipher.encrypt("123456789");char c=encrypted.charAt(encrypted.length()-1);String tampered=encrypted.substring(0,encrypted.length()-1)+(c=='A'?'B':'A');assertThrows(RuntimeException.class,()->AccountNumberCipher.decrypt(tampered));}
 @Test void maskKeepsOnlyLastFourDigits(){assertEquals("******6789",AccountNumberCipher.mask("123456789"));}
}
