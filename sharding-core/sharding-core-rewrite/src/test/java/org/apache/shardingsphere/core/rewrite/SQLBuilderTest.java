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

package org.apache.shardingsphere.core.rewrite;

import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import org.apache.shardingsphere.core.parse.antlr.constant.QuoteCharacter;
import org.apache.shardingsphere.core.rewrite.placeholder.IndexPlaceholder;
import org.apache.shardingsphere.core.rewrite.placeholder.SchemaPlaceholder;
import org.apache.shardingsphere.core.rewrite.placeholder.TablePlaceholder;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class SQLBuilderTest {
    
    private ShardingDataSourceMetaData shardingDataSourceMetaData;
    
    @Before
    public void setUp() {
        Map<String, String> shardingDataSourceURLs = new LinkedHashMap<>();
        shardingDataSourceURLs.put("ds0", "jdbc:mysql://127.0.0.1:3306/actual_db");
        shardingDataSourceURLs.put("ds1", "jdbc:mysql://127.0.0.1:3306/actual_db");
        shardingDataSourceMetaData = new ShardingDataSourceMetaData(shardingDataSourceURLs, createShardingRule(), DatabaseType.MySQL);
    }
    
    @Test
    public void assertAppendLiteralsOnly() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendLiterals("table_x");
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendLiterals("table_x");
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("SELECT table_x.id FROM table_x"));
    }
    
    @Test
    public void assertAppendTableWithoutTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("SELECT table_x.id FROM table_x"));
    }
    
    @Test
    public void assertAppendTableWithTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SELECT table_x_1.id FROM table_x_1"));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithoutTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "index_name"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals(" ('column')");
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("CREATE INDEX index_name ON table_x ('column')"));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "table_x"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals(" ('column')");
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("CREATE INDEX index_name_table_x_1 ON table_x_1 ('column')"));
    }
    
    @Test(expected = ShardingException.class)
    public void assertSchemaPlaceholderAppendTableWithoutTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SHOW ");
        sqlBuilder.appendLiterals("CREATE TABLE ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals("ON ");
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder("dx", "table_x", createShardingRule(), null));
        sqlBuilder.toSQL(null, Collections.<String, String>emptyMap());
    }
    
    @Test
    public void assertSchemaPlaceholderAppendTableWithTableToken() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SHOW ");
        sqlBuilder.appendLiterals("CREATE TABLE ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_0", QuoteCharacter.NONE));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder("ds0", "table_0", createShardingRule(), shardingDataSourceMetaData));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_0", "table_1");
//        ShardingDataSourceMetaData shardingDataSourceMetaData = Mockito.mock(ShardingDataSourceMetaData.class);
//        Mockito.when(shardingDataSourceMetaData.getActualSchemaName(Mockito.anyString())).thenReturn("actual_db");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SHOW CREATE TABLE table_1 ON actual_db"));
    }
    
    @Test
    public void assertAppendTableWithoutTableTokenWithBackQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("SELECT `table_x`.id FROM `table_x`"));
    }
    
    @Test
    public void assertAppendTableWithTableTokenWithBackQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SELECT `table_x_1`.id FROM `table_x_1`"));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithoutTableTokenWithBackQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "index_name"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        sqlBuilder.appendLiterals(" ('column')");
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("CREATE INDEX index_name ON `table_x` ('column')"));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithTableTokenWithBackQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "table_x"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.BACK_QUOTE));
        sqlBuilder.appendLiterals(" ('column')");
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("CREATE INDEX index_name_table_x_1 ON `table_x_1` ('column')"));
    }
    
    @Test
    public void assertSchemaPlaceholderAppendTableWithTableTokenWithBackQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SHOW ");
        sqlBuilder.appendLiterals("CREATE TABLE ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_0", QuoteCharacter.BACK_QUOTE));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder("ds", "table_0", createShardingRule(), shardingDataSourceMetaData));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_0", "table_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SHOW CREATE TABLE `table_1` ON actual_db"));
    }
    
    @Test
    public void assertAppendTableWithoutTableTokenWithDoubleQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("SELECT \"table_x\".id FROM \"table_x\""));
    }
    
    @Test
    public void assertAppendTableWithTableTokenWithDoubleQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SELECT ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        sqlBuilder.appendLiterals(".id");
        sqlBuilder.appendLiterals(" FROM ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SELECT \"table_x_1\".id FROM \"table_x_1\""));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithoutTableTokenWithDoubleQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "index_name"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        sqlBuilder.appendLiterals(" ('column')");
        assertThat(sqlBuilder.toSQL(null, Collections.<String, String>emptyMap()).getSql(), is("CREATE INDEX index_name ON \"table_x\" ('column')"));
    }
    
    @Test
    public void assertIndexPlaceholderAppendTableWithTableTokenWithDoubleQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("CREATE INDEX ");
        sqlBuilder.appendPlaceholder(new IndexPlaceholder("index_name", "table_x"));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_x", QuoteCharacter.QUOTE));
        sqlBuilder.appendLiterals(" ('column')");
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_x", "table_x_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("CREATE INDEX index_name_table_x_1 ON \"table_x_1\" ('column')"));
    }
    
    @Test
    public void assertSchemaPlaceholderAppendTableWithTableTokenWithDoubleQuotes() {
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals("SHOW ");
        sqlBuilder.appendLiterals("CREATE TABLE ");
        sqlBuilder.appendPlaceholder(new TablePlaceholder("table_0", QuoteCharacter.QUOTE));
        sqlBuilder.appendLiterals(" ON ");
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder("ds", "table_0", createShardingRule(), shardingDataSourceMetaData));
        Map<String, String> tableTokens = new HashMap<>(1, 1);
        tableTokens.put("table_0", "table_1");
        assertThat(sqlBuilder.toSQL(null, tableTokens).getSql(), is("SHOW CREATE TABLE \"table_1\" ON actual_db"));
    }
    
    @Test
    public void assertShardingPlaceholderToString() {
        assertThat(new IndexPlaceholder("index_name", "table_x").toString(null, Collections.<String, String>emptyMap()), is("index_name"));
        assertThat(new TablePlaceholder("table_name", QuoteCharacter.BACK_QUOTE).toString(null, Collections.<String, String>emptyMap()), is("`table_name`"));
    }
    
    private ShardingRule createShardingRule() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        TableRuleConfiguration tableRuleConfig = new TableRuleConfiguration("LOGIC_TABLE", "ds${0..1}.table_${0..2}");
        shardingRuleConfig.getTableRuleConfigs().add(tableRuleConfig);
        return new ShardingRule(shardingRuleConfig, createDataSourceNames());
    }
    
    private Collection<String> createDataSourceNames() {
        return Arrays.asList("ds0", "ds1");
    }
}
