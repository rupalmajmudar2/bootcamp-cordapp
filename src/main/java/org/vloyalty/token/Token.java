package org.vloyalty.token;

import net.corda.core.serialization.CordaSerializable;

import java.io.Serializable;

//@see https://stackoverflow.com/questions/47476191/class-is-not-annotated-or-on-the-whitelist-so-cannot-be-used-in-serialization
@CordaSerializable
public class Token implements Serializable {
    //private int _numTokens;

    public String toString() {
        return " Points";
        //return _numTokens + " KOINs";
    }

    /*public Token(int numTokens) {
        _numTokens= numTokens;
    }*/
}
