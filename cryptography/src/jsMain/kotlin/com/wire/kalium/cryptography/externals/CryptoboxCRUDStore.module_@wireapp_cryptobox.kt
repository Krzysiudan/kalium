@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

package com.wire.kalium.cryptography.externals

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

external open class CryptoboxCRUDStore(engine: CRUDEngineBaseCollection) : PreKeyStore {
    open var engine: Any
    open var from_store: Any
    open var to_store: Any
    open fun delete_all(): Promise<Boolean>
    override fun delete_prekey(prekeyId: Number): Promise<Number>
    open fun load_identity(): Promise<IdentityKeyPair?>
    override open fun load_prekey(prekeyId: Number): Promise<PreKey?>
    open fun load_prekeys(): Promise<Array<PreKey>>
    open fun save_identity(identity: IdentityKeyPair): Promise<IdentityKeyPair>
    open fun save_prekey(preKey: PreKey): Promise<PreKey>
    open fun save_prekeys(preKeys: Array<PreKey>): Promise<Array<PreKey>>

    companion object {
        var KEYS: Any
        var STORES: Any
    }
}
