package com.starrocks.connector.spark.sql.schema;

import com.starrocks.connector.spark.sql.conf.StarRocksConfig;
import com.starrocks.connector.spark.sql.connect.StarRocksConnector;

import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class InferSchema {

    public static StructType inferSchema(final CaseInsensitiveStringMap options) {
        StarRocksConfig config = StarRocksConfig.createConfig(options);

        List<StarRocksField> inferFields = config.inferFields();
        if (!inferFields.isEmpty()) {
            return inferSchema(inferFields);
        }

        StarRocksSchema schema = StarRocksConnector.getSchema(config);

        if (config.getColumns() != null && config.getColumns().length > 0) {
            return inferSchema(schema.sortAndListField(config.getColumns()));
        }

        return inferSchema(schema.getFieldMap().values());
    }

    static StructType inferSchema(Collection<StarRocksField> srFields) {

        List<StructField> fields = srFields.stream()
                .map(InferSchema::inferStructField)
                .collect(Collectors.toList());

        return DataTypes.createStructType(fields);
    }

    static StructField inferStructField(StarRocksField field) {
        DataType dataType = inferDataType(field);

        return new StructField(field.getName(), dataType, true, Metadata.empty());
    }

    static DataType inferDataType(StarRocksField field) {
        String type = field.getType().toLowerCase(Locale.ROOT);

        switch (type) {
            case "largeint":
            case "bigint":
                return DataTypes.LongType;
            case "tinyint":
            case "smallint":
            case "int":
                return DataTypes.IntegerType;
            case "boolean":
                return DataTypes.BooleanType;
            case "float":
                return DataTypes.FloatType;
            case "double":
                return DataTypes.DoubleType;
            case "decimal":
                return DataTypes.createDecimalType(Integer.parseInt(field.getSize()), Integer.parseInt(field.getScale()));
            case "char":
            case "varchar":
            case "json":
            case "string":
                return DataTypes.StringType;
            case "date":
                return DataTypes.DateType;
            case "datetime":
                return DataTypes.TimestampType;
            default:
                return DataTypes.NullType;
        }
    }
}
