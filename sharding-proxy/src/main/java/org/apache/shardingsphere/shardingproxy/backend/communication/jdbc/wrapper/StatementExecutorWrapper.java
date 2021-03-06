/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingproxy.backend.communication.jdbc.wrapper;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import org.apache.shardingsphere.core.parsing.SQLJudgeEngine;
import org.apache.shardingsphere.core.parsing.parser.sql.SQLStatement;
import org.apache.shardingsphere.core.rewrite.MasterSlaveSQLRewriteEngine;
import org.apache.shardingsphere.core.routing.RouteUnit;
import org.apache.shardingsphere.core.routing.SQLRouteResult;
import org.apache.shardingsphere.core.routing.SQLUnit;
import org.apache.shardingsphere.core.routing.StatementRoutingEngine;
import org.apache.shardingsphere.core.routing.router.masterslave.MasterSlaveRouter;
import org.apache.shardingsphere.shardingproxy.runtime.GlobalRegistry;
import org.apache.shardingsphere.shardingproxy.runtime.schema.LogicSchema;
import org.apache.shardingsphere.shardingproxy.runtime.schema.MasterSlaveSchema;
import org.apache.shardingsphere.shardingproxy.runtime.schema.ShardingSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * Executor wrapper for statement.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class StatementExecutorWrapper implements JDBCExecutorWrapper {
    
    private static final GlobalRegistry GLOBAL_REGISTRY = GlobalRegistry.getInstance();
    
    private final LogicSchema logicSchema;
    
    @Override
    public SQLRouteResult route(final String sql, final DatabaseType databaseType) {
        return logicSchema instanceof MasterSlaveSchema ? doMasterSlaveRoute(sql) : doShardingRoute(sql, databaseType);
    }
    
    private SQLRouteResult doMasterSlaveRoute(final String sql) {
        SQLStatement sqlStatement = new SQLJudgeEngine(sql).judge();
        String rewriteSQL = new MasterSlaveSQLRewriteEngine(((MasterSlaveSchema) logicSchema).getMasterSlaveRule(), sql, sqlStatement, logicSchema.getMetaData()).rewrite();
        SQLRouteResult result = new SQLRouteResult(sqlStatement);
        for (String each : new MasterSlaveRouter(
                ((MasterSlaveSchema) logicSchema).getMasterSlaveRule(), GLOBAL_REGISTRY.getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW)).route(rewriteSQL)) {
            result.getRouteUnits().add(new RouteUnit(each, new SQLUnit(rewriteSQL, Collections.<List<Object>>emptyList())));
        }
        return result;
    }
    
    private SQLRouteResult doShardingRoute(final String sql, final DatabaseType databaseType) {
        StatementRoutingEngine routingEngine = new StatementRoutingEngine(((ShardingSchema) logicSchema).getShardingRule(), logicSchema.getMetaData(),
                databaseType, GLOBAL_REGISTRY.getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW));
        return routingEngine.route(sql);
    }
    
    @Override
    public Statement createStatement(final Connection connection, final SQLUnit sqlUnit, final boolean isReturnGeneratedKeys) throws SQLException {
        return connection.createStatement();
    }
    
    @Override
    public boolean executeSQL(final Statement statement, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        return statement.execute(sql, isReturnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
    }
}
