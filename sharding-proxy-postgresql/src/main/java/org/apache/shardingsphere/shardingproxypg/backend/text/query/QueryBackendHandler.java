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

package org.apache.shardingsphere.shardingproxypg.backend.text.query;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.shardingproxypg.backend.ResultPacket;
import org.apache.shardingsphere.shardingproxypg.backend.communication.DatabaseCommunicationEngine;
import org.apache.shardingsphere.shardingproxypg.backend.communication.DatabaseCommunicationEngineFactory;
import org.apache.shardingsphere.shardingproxypg.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.shardingproxypg.backend.text.TextProtocolBackendHandler;
import org.apache.shardingsphere.shardingproxypg.transport.postgresql.packet.command.PostgreSQLCommandResponsePackets;
import org.apache.shardingsphere.shardingproxypg.transport.postgresql.packet.generic.PostgreSQLErrorResponsePacket;

import java.sql.SQLException;

/**
 * Backend handler with query.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class QueryBackendHandler implements TextProtocolBackendHandler {
    
    private final DatabaseCommunicationEngineFactory databaseCommunicationEngineFactory = DatabaseCommunicationEngineFactory.getInstance();
    
    private final int sequenceId;
    
    private final String sql;
    
    private final BackendConnection backendConnection;
    
    private final DatabaseType databaseType;
    
    private DatabaseCommunicationEngine databaseCommunicationEngine;
    
    @Override
    public PostgreSQLCommandResponsePackets execute() {
        if (null == backendConnection.getLogicSchema()) {
            return new PostgreSQLCommandResponsePackets(new PostgreSQLErrorResponsePacket());
        }
        databaseCommunicationEngine = databaseCommunicationEngineFactory.newTextProtocolInstance(backendConnection.getLogicSchema(), sequenceId, sql, backendConnection, databaseType);
        return databaseCommunicationEngine.execute();
    }
    
    @Override
    public boolean next() throws SQLException {
        return databaseCommunicationEngine.next();
    }
    
    @Override
    public ResultPacket getResultValue() throws SQLException {
        return databaseCommunicationEngine.getResultValue();
    }
}
