package com.example.superinvoice

import com.example.superinvoice.data.analytics.ConsentRegion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O país da rede não pode ser forjado no aparelho, então o caminho europeu do padrão
 * regional só é verificável aqui.
 */
class ConsentRegionTest {

    @Test
    fun `paises do EEE exigem opt-in`() {
        listOf("DE", "FR", "IE", "PT", "ES", "IT", "NL", "SE", "PL").forEach {
            assertTrue("$it deveria exigir opt-in", ConsentRegion.requiresOptIn(it))
        }
    }

    @Test
    fun `reino unido, gibraltar e suica exigem opt-in`() {
        listOf("GB", "GI", "CH").forEach {
            assertTrue("$it deveria exigir opt-in", ConsentRegion.requiresOptIn(it))
        }
    }

    @Test
    fun `fora do EEE nao exige opt-in`() {
        listOf("BR", "US", "CA", "MX", "AR", "JP", "AU", "IN").forEach {
            assertFalse("$it nao deveria exigir opt-in", ConsentRegion.requiresOptIn(it))
        }
    }

    /** Tablet sem SIM e sem país no locale: erra para o lado de coletar menos. */
    @Test
    fun `pais indeterminado cai para opt-in`() {
        assertTrue(ConsentRegion.requiresOptIn(null))
        assertTrue(ConsentRegion.requiresOptIn(""))
        assertTrue(ConsentRegion.requiresOptIn("   "))
    }

    /** networkCountryIso costuma vir minúsculo. */
    @Test
    fun `comparacao ignora caixa`() {
        assertTrue(ConsentRegion.requiresOptIn("de"))
        assertFalse(ConsentRegion.requiresOptIn("br"))
    }
}
