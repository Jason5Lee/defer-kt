package me.jason5lee.defer

/**
 * Thrown when the defer scope is used out of scope or cancelled.
 */
public class DeferScopeClosedException(scopeName: String) : IllegalStateException("$scopeName used out of scope or cancelled")
