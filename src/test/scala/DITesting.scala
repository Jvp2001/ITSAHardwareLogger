
class DatabaseAccess()
class SecurityFilter()
class UserFinder(databaseAccess: DatabaseAccess, securityFilter: SecurityFilter)
class UserStatusReader(userFinder: UserFinder)


import com.softwaremill.macwire.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite


class Tests extends AnyFunSuite:
  test("DependencyInjection"):
    val userStatusReader = autowire[UserStatusReader]()
    assert(userStatusReader != null)