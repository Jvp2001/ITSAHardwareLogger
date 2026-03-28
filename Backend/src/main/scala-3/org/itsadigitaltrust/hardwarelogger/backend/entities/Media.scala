package org.itsadigitaltrust.hardwarelogger.backend.entities

import com.augustnagro.magnum.*

@Table(MySqlDbType)
@SqlName("media")
final case class Media(@Id id: Long, 
                       @SqlName("itsaid") itsaID: String,
                       @SqlName("description") descr: Option[String],
                       handle: Option[String]
                      ) extends HLEntityWithItsaID derives DbCodec

final case class MediaCreator(@SqlName("itsaid") itsaID: String, @SqlName("description") descr: Option[String], handle: Option[String] = None) extends HLEntityCreatorWithItsaID derives DbCodec