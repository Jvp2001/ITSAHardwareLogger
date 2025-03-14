package org.itsadigitaltrust.common
package intrepolators

import scala.util.matching.Regex

extension(sc: StringContext)
    def r(args: Any*): Regex = 
        raw"${sc.s(args)}".r



  
