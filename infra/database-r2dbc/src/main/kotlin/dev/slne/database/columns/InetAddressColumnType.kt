package dev.slne.database.columns

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Table.Dual.registerColumn
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.net.InetAddress

class InetAddressColumnType : ColumnType<InetAddress>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.varcharType(45)

    override fun valueFromDB(value: Any): InetAddress = when (value) {
        is InetAddress -> value
        is String -> InetAddress.getByName(value)
        is ByteArray -> InetAddress.getByAddress(value)
        else -> error("Cannot convert $value to InetAddress")
    }

    override fun notNullValueToDB(value: InetAddress): Any = value.hostAddress

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value is InetAddress) {
            stmt.set(index, value.hostAddress, this)
        } else {
            super.setParameter(stmt, index, value)
        }
    }
}

fun Table.inet(name: String): Column<InetAddress> = registerColumn(name, InetAddressColumnType())