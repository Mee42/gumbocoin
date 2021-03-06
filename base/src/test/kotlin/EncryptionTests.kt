import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import systems.carson.base.Person
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.IllegalBlockSizeException

internal class EncryptionTests {
    @Test
    fun `Deterministic public key generation from private key`() {
        val person1 = privateBob()
        val person2 = Person.fromPrivateKey(person1.getPrivateKeyy())
        assertEquals(person1, person2)
    }

    @Test
    fun `person#equals`() {
        val person = privateBob()
        assertEquals(person, person)
    }

    @Test
    fun `Deterministic generation works`() {
        assertEquals(privateBob(), privateBob())
    }

    @Test
    fun `Deterministic generation is based on name`() {
        val a = privateBob()
        val b = privateAlice()
        assertNotEquals(a, b)
    }

    @Test
    fun `Different people are not identical`() {
        assertNotEquals(privateBob(), privateAlice())
    }

    @Test
    fun `Non-deterministic generation generates different keys`() {
        assertNotEquals(privateBobRandom(), privateBobRandom())
    }

    @Test
    fun `Verify signature with only the public key`() {
        val publicBob = publicBob()//does not have the private key
        val privateBob = privateBob()//has the private key

        //bob is going to sign a thing
        val data = randomData()
        val signature = privateBob.sign(data)
        assertTrue(Person.verify(publicBob, signature, data))
    }

    @Test
    fun `Can't sign without a private key`() {
        assertThrows(
            IllegalStateException::class.java,
            { publicBobRandom().sign(randomData()) },
            "Expected sign without private key to failed"
        )
    }

    @Test
    fun `Can sign with a private key`() {
        privateBobRandom().sign(randomData())
    }

    @Test
    fun `Signature is not valid when compared to the wrong public key`() {
        val alice = publicAliceRandom()
        val bob = privateBobRandom()
        val data = randomData()
        val sig = bob.sign(data)
        assertFalse(Person.verify(alice, sig, data))
    }

    @Test
    fun `Signatures for identical data is identical`() {
        val bob = privateBobRandom()
        val data = randomData()
        val sig1 = bob.sign(data)
        val sig2 = bob.sign(data)
        assertEquals(sig1, sig2)
    }

    @Test
    fun `Signatures from different people are different`() {
        val alice = privateAliceRandom()
        val bob = privateBobRandom()
        val data = randomData()
        val sig1 = alice.sign(data)
        val sig2 = bob.sign(data)
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun `Signatures for different data are different`() {
        val bob = privateBobRandom()
        val sig1 = bob.sign(randomData())
        val sig2 = bob.sign(randomData())
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun `Encrypt with RSA`() {
        val private = privateBob()
        val public = publicBob()

        val data = randomData()
        val encrypted = Person.encryptRSA(data, public)
        assertNotEquals(data, encrypted)
        val plaintext = private.decryptRSA(encrypted)
        assertEquals(data.contentToString(), plaintext.contentToString())
    }

    @Test
    fun `Can't decrypt without the private key`() {
        val public = publicBobRandom()
        val encrypted = Person.encryptRSA(randomData(), public)
        assertThrows(IllegalStateException::class.java) { public.decryptRSA(encrypted) }
    }

    @Test
    fun `Can't decrypt with the wrong private key`() {
        val public = publicBob()
        val private = privateAlice()
        val data = randomData()
        val encrypted = Person.encryptRSA(data, public)
        val message = assertThrows(IllegalAccessException::class.java) { private.decryptRSA(encrypted) }.message
        assertEquals("Unable to decryptRSA data with given key", message)
    }

    @Test
    fun `Can encrypt exactly MAX_RSA bytes`() {
        val data = randomData(Person.MAX_RSA_BYTES)
        val bob = privateBob()
        Person.encryptRSA(data, bob)
    }

    @Test
    fun `Can't encrypt more then MAX_RSA bytes`() {
        val data = randomData(Person.MAX_RSA_BYTES + 1)
        val bob = privateBob()
        assertThrows(IllegalBlockSizeException::class.java) { Person.encryptRSA(data, bob) }
    }

    @Test
    fun `AES encryption works`() {
        val data = randomData()
        val bob = privateBob()
        val plaintext = bob.decryptAES(Person.encryptAES(data, bob))
        assertByteArraysEquals(data, plaintext)
    }

    @Test
    fun `AES encryption works with more bytes then MAX_RSA`() {
        val data = randomData(Person.MAX_RSA_BYTES * 100)
        val bob = privateBob()
        val plaintext = bob.decryptAES(Person.encryptAES(data, bob))
        assertByteArraysEquals(data, plaintext)
    }

    @Test
    fun `AES works with 0 bytes`() {
        val data = ByteArray(0) { 0 }
        val bob = privateBob()
        val plaintext = bob.decryptAES(Person.encryptAES(data, bob))
        assertByteArraysEquals(data, plaintext)
    }

    @Test
    fun `RSA works with 0 bytes`() {
        val data = ByteArray(0) { 0 }
        val bob = privateBob()
        val plaintext = bob.decryptRSA(Person.encryptRSA(data, bob))
        assertByteArraysEquals(data, plaintext)
    }

    @Test
    fun `Person from keypair`() {
        val example = privateBob()
        val person = Person.fromKeyPair(KeyPair(example.publicKey, example.getPrivateKeyy()))
        assert(example.isValid())
        assert(person.isValid())
    }

    @Test
    fun `Fingerprints are deterministic`() {
        val bob1 = privateBob()
        val bob2 = privateBob()
        assertEquals(bob1.fingerprint(), bob2.fingerprint())
    }

    @Test
    fun `Fingerprints are determined by the public key only`() {
        val bob = privateBob()
        val alice = privateAlice()

        val p1 = Person.fromKeyPair(KeyPair(bob.publicKey, alice.getPrivateKeyy()))
        assertEquals(bob.fingerprint(), p1.fingerprint())
    }

    @Test
    fun `toString doesn't include anything that uses the private key`() {
        val bob = privateBob()
        val alice = privateAlice()

        val p1 = Person.fromKeyPair(KeyPair(bob.publicKey, alice.getPrivateKeyy()))
        assertEquals(bob.toString(), p1.toString())
    }


    @Test
    fun `Can generate private key externally and have it work`() {
        val keySize = 2048
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize)
        val keys = keyPairGenerator.genKeyPair()

        val publicKey = Person.fromPrivateKey(keys.private).publicKey

        assertByteArraysEquals(keys.public.encoded, publicKey.encoded)
    }

    @Test
    fun `Can't generate with a different key type`() {

        val keyPairGenerator = KeyPairGenerator.getInstance("DSA")
        keyPairGenerator.initialize(2048)

        assertThrows(IllegalStateException::class.java) { Person.fromPrivateKey(keyPairGenerator.genKeyPair().private) }

    }


    @Test
    fun `Encryption a large amount of data`() {
        val data = randomData(1028 * 1028)//1 mb
        val bob = privateBob()
        val encryptedData = Person.encryptAES(data, bob)
        val plaintext = bob.decryptAES(encryptedData)
        assertByteArraysEquals(data, plaintext)
    }


    @Test
    fun `Hash codes are deterministic`() {
        assertEquals(privateBob().hashCode(), privateBob().hashCode())
    }

    @Test
    fun `Hash codes are different`() {
        assertNotEquals(privateAlice().hashCode(), privateBob().hashCode())
    }

    @Test
    fun `Signature hash codes are deterministic`() {
        val bob = privateBob()
        val data = randomData()
        val sig1 = bob.sign(data)
        val sig2 = bob.sign(data)
        assertEquals(sig1.hashCode(), sig2.hashCode())
    }

    @Test
    fun `Signature hash codes are different`() {
        val bob = privateBob()
        val sig1 = bob.sign(randomData())
        val sig2 = bob.sign(randomData())
        assertNotEquals(sig1.hashCode(), sig2.hashCode())
    }


    private fun assertByteArraysEquals(one: ByteArray, two: ByteArray) {
        assertEquals(one.contentToString(), two.contentToString())
    }


    fun randomData(size: Int = 128) = ByteArray(size) { (Byte.MIN_VALUE..Byte.MAX_VALUE).random().toByte() }


    private object Lazzy {
        val privateBob by lazy { Person.deterministicFromString("Bob") }
        val privateAlice by lazy { Person.deterministicFromString("Alice") }
        val privateEve by lazy { Person.deterministicFromString("Eve") }
        val publicBob by lazy { Person.fromPublicKey(Person.deterministicFromString("Bob").publicKey) }
        val publicAlice by lazy { Person.fromPublicKey(Person.deterministicFromString("Alice").publicKey) }
        val publicEve by lazy { Person.fromPublicKey(Person.deterministicFromString("Eve").publicKey) }

    }

    companion object {
        fun privateBob() = Lazzy.privateBob
        fun privateAlice() = Lazzy.privateAlice
        fun privateEve() = Lazzy.privateEve

        fun publicBob() = Lazzy.publicBob
        fun publicAlice() = Lazzy.publicAlice
        fun publicEve() = Lazzy.publicEve

        fun privateBobRandom() = Person.generateNew()
        fun privateAliceRandom() = Person.generateNew()
        fun privateEveRandom() = Person.generateNew()

        fun publicEveRandom() = Person.fromPublicKey(Person.generateNew().publicKey)
        fun publicBobRandom() = Person.fromPublicKey(Person.generateNew().publicKey)
        fun publicAliceRandom() = Person.fromPublicKey(Person.generateNew().publicKey)
    }
}
