package com.mdevs.trackera.config.database;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class TrackeraTableNamingStrategy extends PhysicalNamingStrategyStandardImpl {
    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context) {
        if (logicalName == null) {
            return null;
        }
        return Identifier.toIdentifier(pluralize(logicalName.getText()).toUpperCase());
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier identifier, JdbcEnvironment jdbcEnvironment) {
        return convertToUpperSnakeCase(identifier);
    }

    private String pluralize(String singular) {
        if (StringUtils.isEmpty(singular)) {
            return singular;
        }

        singular = singular.toLowerCase();
        if (singular.endsWith("s")) {
            return singular;
        }

        if (singular.endsWith("sh") || singular.endsWith("ch") || singular.endsWith("x") || singular.endsWith("z")) {
            return singular + "es";
        }

        if (singular.endsWith("y") && singular.length() > 1) {
            char beforeY = singular.charAt(singular.length() - 2);
            if (!"aeiou".contains(String.valueOf(beforeY))) {
                return singular.substring(0, singular.length() - 1) + "ies";
            }
        }

        return singular + "s";
    }

    private Identifier convertToUpperSnakeCase(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        return Identifier.toIdentifier(identifier.getText().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
    }
}
