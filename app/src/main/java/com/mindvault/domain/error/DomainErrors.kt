package com.mindvault.domain.error

class DuplicateNameException(val name: String) :
    Exception("\"$name\" already exists")

class DefaultDeckProtectedException :
    Exception("The default deck cannot be deleted")