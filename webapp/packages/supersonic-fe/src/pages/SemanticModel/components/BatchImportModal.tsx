import React, { useState, useEffect, useRef } from 'react';
import {
  Modal,
  Form,
  Select,
  Table,
  Button,
  Spin,
  message,
  Steps,
  Space,
  Card,
  Tabs,
  Tag,
  Row,
  Col,
  Input,
  Switch,
  Tooltip,
  Radio,
} from 'antd';
import {
  DatabaseOutlined,
  TableOutlined,
  RobotOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  FileTextOutlined,
  ImportOutlined,
} from '@ant-design/icons';
import {
  getDatabaseList,
  getCatalogs,
  getDbNames,
  getTables,
  buildModelSchema,
  createModelBatch,
} from '../service';
import { getLlmList } from '@/services/system';
import { useModel } from '@umijs/max';
import { ISemantic } from '../data';
import styles from './style.less';

const { Step } = Steps;
const { TabPane } = Tabs;
const { Search } = Input;
const { TextArea } = Input;
const FormItem = Form.Item;

type ImportMode = 'database' | 'ddl';

const FIELD_TYPE_LABELS: Record<string, string> = {
  primary_key: '主键',
  foreign_key: '外键',
  partition_time: '分区时间',
  time: '时间',
  categorical: '维度',
  measure: '度量',
};

const FIELD_TYPE_COLORS: Record<string, string> = {
  primary_key: 'purple',
  foreign_key: 'cyan',
  partition_time: 'orange',
  time: 'gold',
  categorical: 'blue',
  measure: 'green',
};

type Props = {
  open: boolean;
  onCancel: () => void;
  onSubmit: () => void;
};

const BatchImportModal: React.FC<Props> = ({ open, onCancel, onSubmit }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [creating, setCreating] = useState(false);

  const [importMode, setImportMode] = useState<ImportMode>('database');

  const [databaseList, setDatabaseList] = useState<ISemantic.IDatabaseItem[]>([]);
  const [catalogList, setCatalogList] = useState<string[]>([]);
  const [dbNameList, setDbNameList] = useState<string[]>([]);
  const [tableNameList, setTableNameList] = useState<string[]>([]);
  const [llmList, setLlmList] = useState<ISemantic.ILlmItem[]>([]);

  const [selectedDatabaseId, setSelectedDatabaseId] = useState<number>();
  const [selectedCatalog, setSelectedCatalog] = useState<string>('');
  const [selectedDbName, setSelectedDbName] = useState<string>('');
  const [selectedTables, setSelectedTables] = useState<string[]>([]);
  const [selectedLlmId, setSelectedLlmId] = useState<number>();
  const [useLLM, setUseLLM] = useState(true);

  const [ddlText, setDdlText] = useState<string>('');
  const [parsedTables, setParsedTables] = useState<string[]>([]);

  const [modelSchemas, setModelSchemas] = useState<Record<string, ISemantic.IModelSchema>>({});
  const [searchTableText, setSearchTableText] = useState('');
  const [activeTabKey, setActiveTabKey] = useState<string>('');

  const tableSearchRef = useRef<string>('');

  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId } = domainModel;

  useEffect(() => {
    if (open) {
      queryDatabaseList();
      queryLlmList();
      resetState();
    }
  }, [open]);

  const resetState = () => {
    setCurrentStep(0);
    setImportMode('database');
    setSelectedDatabaseId(undefined);
    setSelectedCatalog('');
    setSelectedDbName('');
    setSelectedTables([]);
    setSelectedLlmId(undefined);
    setUseLLM(true);
    setDdlText('');
    setParsedTables([]);
    setModelSchemas({});
    setSearchTableText('');
    setActiveTabKey('');
    form.resetFields();
  };

  const queryDatabaseList = async () => {
    setLoading(true);
    const { code, data, msg } = await getDatabaseList();
    setLoading(false);
    if (code === 200) {
      setDatabaseList(data || []);
    } else {
      message.error(msg || '获取数据库列表失败');
    }
  };

  const queryLlmList = async () => {
    const { code, data, msg } = await getLlmList();
    if (code === 200) {
      setLlmList(data || []);
      if (data && data.length > 0) {
        setSelectedLlmId(data[0].id);
      }
    } else {
      message.error(msg || '获取LLM配置列表失败');
    }
  };

  const queryCatalogList = async (databaseId: number) => {
    setLoading(true);
    const { code, data, msg } = await getCatalogs(databaseId);
    setLoading(false);
    if (code === 200) {
      setCatalogList(data || []);
    } else {
      message.error(msg || '获取Catalog列表失败');
    }
  };

  const queryDbNameList = async (databaseId: number, catalog: string) => {
    setLoading(true);
    const { code, data, msg } = await getDbNames(databaseId, catalog);
    setLoading(false);
    if (code === 200) {
      setDbNameList(data || []);
    } else {
      message.error(msg || '获取数据库名列表失败');
    }
  };

  const queryTableNameList = async (databaseId: number, catalog: string, dbName: string) => {
    setLoading(true);
    const { code, data, msg } = await getTables(databaseId, catalog, dbName);
    setLoading(false);
    if (code === 200) {
      setTableNameList(data || []);
      setSelectedTables([]);
    } else {
      message.error(msg || '获取表列表失败');
    }
  };

  const handleDatabaseSelect = (databaseId: number, option: any) => {
    setSelectedDatabaseId(databaseId);
    setSelectedCatalog('');
    setSelectedDbName('');
    setSelectedTables([]);
    setCatalogList([]);
    setDbNameList([]);
    setTableNameList([]);

    const type = option?.type || '';
    if (['STARROCKS', 'KYUUBI', 'PRESTO', 'TRINO'].includes(type)) {
      queryCatalogList(databaseId);
    } else {
      queryDbNameList(databaseId, '');
    }
  };

  const handleCatalogSelect = (catalog: string) => {
    setSelectedCatalog(catalog);
    setSelectedDbName('');
    setSelectedTables([]);
    setDbNameList([]);
    setTableNameList([]);
    if (selectedDatabaseId) {
      queryDbNameList(selectedDatabaseId, catalog);
    }
  };

  const handleDbNameSelect = (dbName: string) => {
    setSelectedDbName(dbName);
    setSelectedTables([]);
    setTableNameList([]);
    if (selectedDatabaseId !== undefined) {
      queryTableNameList(selectedDatabaseId, selectedCatalog, dbName);
    }
  };

  const filteredTableList = searchTableText
    ? tableNameList.filter((table) =>
        table.toLowerCase().includes(searchTableText.toLowerCase()),
      )
    : tableNameList;

  const tableColumns = [
    {
      title: '表名',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => (
        <Space>
          <TableOutlined />
          {text}
        </Space>
      ),
    },
  ];

  const tableDataSource = filteredTableList.map((table) => ({
    key: table,
    name: table,
  }));

  const rowSelection = {
    selectedRowKeys: selectedTables,
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedTables(selectedRowKeys as string[]);
    },
  };

  const handleNext = () => {
    if (currentStep === 0) {
      if (importMode === 'database') {
        if (!selectedDatabaseId) {
          message.error('请选择数据库连接');
          return;
        }
        if (!selectedDbName) {
          message.error('请选择数据库名');
          return;
        }
        if (selectedTables.length === 0) {
          message.error('请至少选择一个表');
          return;
        }
      } else {
        if (!ddlText.trim()) {
          message.error('请输入建表语句');
          return;
        }
      }
    }
    setCurrentStep(currentStep + 1);
  };

  const handlePrev = () => {
    setCurrentStep(currentStep - 1);
  };

  const handleAnalyze = async () => {
    if (importMode === 'database' && selectedTables.length === 0) {
      message.error('请至少选择一个表');
      return;
    }
    if (importMode === 'ddl' && !ddlText.trim()) {
      message.error('请输入建表语句');
      return;
    }

    setAnalyzing(true);
    try {
      const params: any = {
        buildByLLM: useLLM,
        domainId: selectDomainId,
      };

      if (importMode === 'database') {
        params.databaseId = selectedDatabaseId;
        params.catalog = selectedCatalog;
        params.db = selectedDbName;
        params.tables = selectedTables;
      } else {
        params.dbSchemas = [
          {
            ddl: ddlText,
            catalog: selectedCatalog || undefined,
            db: selectedDbName || undefined,
          },
        ];
      }

      if (useLLM && selectedLlmId) {
        params.chatModelId = selectedLlmId;
      }

      const { code, data, msg } = await buildModelSchema(params);
      if (code === 200) {
        setModelSchemas(data || {});
        if (data && Object.keys(data).length > 0) {
          setActiveTabKey(Object.keys(data)[0]);
        }
        message.success('语义分析完成');
        setCurrentStep(2);
      } else {
        message.error(msg || '语义分析失败');
      }
    } catch (error: any) {
      message.error(error?.message || '语义分析失败');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleCreate = async () => {
    if (Object.keys(modelSchemas).length === 0) {
      message.error('没有可创建的模型');
      return;
    }

    setCreating(true);
    try {
      const params: any = {
        buildByLLM: useLLM,
        domainId: selectDomainId,
      };

      if (importMode === 'database') {
        params.databaseId = selectedDatabaseId;
        params.catalog = selectedCatalog;
        params.db = selectedDbName;
        params.tables = Object.keys(modelSchemas);
      } else {
        params.dbSchemas = [
          {
            ddl: ddlText,
            catalog: selectedCatalog || undefined,
            db: selectedDbName || undefined,
          },
        ];
      }

      if (useLLM && selectedLlmId) {
        params.chatModelId = selectedLlmId;
      }

      const { code, msg } = await createModelBatch(params);
      if (code === 200) {
        message.success('批量创建模型成功');
        onSubmit();
      } else {
        message.error(msg || '批量创建模型失败');
      }
    } catch (error: any) {
      message.error(error?.message || '批量创建模型失败');
    } finally {
      setCreating(false);
    }
  };

  const getSchemaColumns = (schema: ISemantic.IModelSchema) => {
    if (!schema || !schema.semanticColumns) return [];
    return schema.semanticColumns.map((col, index) => ({
      key: index,
      ...col,
    }));
  };

  const schemaColumns = [
    {
      title: '字段名',
      dataIndex: 'columnName',
      key: 'columnName',
      width: 180,
    },
    {
      title: '中文名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '数据类型',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 120,
    },
    {
      title: '语义类型',
      dataIndex: 'filedType',
      key: 'filedType',
      width: 120,
      render: (type: string) => (
        <Tag color={FIELD_TYPE_COLORS[type] || 'default'}>
          {FIELD_TYPE_LABELS[type] || type}
        </Tag>
      ),
    },
    {
      title: '聚合函数',
      dataIndex: 'agg',
      key: 'agg',
      width: 100,
      render: (agg: string, record: ISemantic.ISemanticColumn) => {
        if (record.filedType === 'measure' && agg && agg !== 'NONE') {
          return <Tag color="green">{agg.toUpperCase()}</Tag>;
        }
        return '-';
      },
    },
    {
      title: '描述',
      dataIndex: 'comment',
      key: 'comment',
      ellipsis: true,
      render: (desc: string) => desc || '-',
    },
  ];

  const steps = [
    {
      title: importMode === 'database' ? '选择表' : '输入建表语句',
      icon: importMode === 'database' ? <DatabaseOutlined /> : <FileTextOutlined />,
    },
    {
      title: 'LLM配置',
      icon: <RobotOutlined />,
    },
    {
      title: '预览结果',
      icon: <EyeOutlined />,
    },
  ];

  const getSelectedCount = () => {
    if (importMode === 'database') {
      return selectedTables.length;
    }
    return parsedTables.length;
  };

  const renderModeSelector = () => (
    <Card size="small" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 12, fontWeight: 500 }}>选择导入方式</div>
      <Radio.Group value={importMode} onChange={(e) => setImportMode(e.target.value)}>
        <Space direction="vertical" size="middle">
          <Radio value="database">
            <Space>
              <DatabaseOutlined />
              <span>从数据库表导入</span>
            </Space>
            <div style={{ marginLeft: 24, marginTop: 4, color: '#666', fontSize: 12 }}>
              需要配置数据库连接，从数据库元数据获取表结构
            </div>
          </Radio>
          <Radio value="ddl">
            <Space>
              <FileTextOutlined />
              <span>从建表语句导入</span>
            </Space>
            <div style={{ marginLeft: 24, marginTop: 4, color: '#666', fontSize: 12 }}>
              无需数据库连接，直接解析 CREATE TABLE 语句
            </div>
          </Radio>
        </Space>
      </Radio.Group>
    </Card>
  );

  const renderDatabaseStep = () => (
    <div className={styles.batchImportStepContent}>
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={8}>
            <FormItem
              name="databaseId"
              label="数据库连接"
              rules={[{ required: true, message: '请选择数据库连接' }]}
            >
              <Select
                showSearch
                placeholder="请选择数据库连接"
                loading={loading}
                onSelect={(value: number, option: any) => handleDatabaseSelect(value, option)}
                optionFilterProp="children"
              >
                {databaseList.map((item) => (
                  <Select.Option
                    key={item.id}
                    value={item.id}
                    type={item.type}
                    disabled={!item.hasUsePermission}
                  >
                    {item.name}
                  </Select.Option>
                ))}
              </Select>
            </FormItem>
          </Col>
          {catalogList.length > 0 && (
            <Col span={8}>
              <FormItem
                name="catalog"
                label="Catalog"
                rules={[{ required: true, message: '请选择Catalog' }]}
              >
                <Select
                  showSearch
                  placeholder="请选择Catalog"
                  loading={loading}
                  onSelect={handleCatalogSelect}
                  value={selectedCatalog || undefined}
                >
                  {catalogList.map((item) => (
                    <Select.Option key={item} value={item}>
                      {item}
                    </Select.Option>
                  ))}
                </Select>
              </FormItem>
            </Col>
          )}
          <Col span={8}>
            <FormItem
              name="dbName"
              label="数据库名"
              rules={[{ required: true, message: '请选择数据库名' }]}
            >
              <Select
                showSearch
                placeholder="请选择数据库名"
                loading={loading}
                onSelect={handleDbNameSelect}
                value={selectedDbName || undefined}
                disabled={dbNameList.length === 0}
              >
                {dbNameList.map((item) => (
                  <Select.Option key={item} value={item}>
                    {item}
                  </Select.Option>
                ))}
              </Select>
            </FormItem>
          </Col>
        </Row>
      </Form>

      {tableNameList.length > 0 && (
        <div style={{ marginTop: 20 }}>
          <div style={{ marginBottom: 12 }}>
            <Space>
              <span>已选择: </span>
              <Tag color="blue">{selectedTables.length} 个表</Tag>
            </Space>
            <Search
              placeholder="搜索表名"
              allowClear
              style={{ width: 300, float: 'right' }}
              onChange={(e) => {
                setSearchTableText(e.target.value);
                tableSearchRef.current = e.target.value;
              }}
              onSearch={(value) => {
                setSearchTableText(value);
                tableSearchRef.current = value;
              }}
            />
          </div>
          <Table
            rowSelection={rowSelection}
            columns={tableColumns}
            dataSource={tableDataSource}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 个表`,
            }}
            size="small"
            scroll={{ y: 350 }}
          />
        </div>
      )}
    </div>
  );

  const renderDDLStep = () => (
    <div className={styles.batchImportStepContent}>
      <Card size="small" style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 8, fontWeight: 500 }}>
          输入建表语句
          <Tooltip title="支持输入多个 CREATE TABLE 语句，用分号分隔">
            <InfoCircleOutlined style={{ marginLeft: 8, color: '#1890ff' }} />
          </Tooltip>
        </div>
        <TextArea
          value={ddlText}
          onChange={(e) => setDdlText(e.target.value)}
          placeholder={`请输入 CREATE TABLE 语句，例如：

CREATE TABLE users (
  id BIGINT PRIMARY KEY COMMENT '用户ID',
  name VARCHAR(100) COMMENT '用户名',
  email VARCHAR(200) COMMENT '邮箱',
  created_at DATETIME COMMENT '创建时间'
);

CREATE TABLE orders (
  order_id BIGINT PRIMARY KEY COMMENT '订单ID',
  user_id BIGINT COMMENT '用户ID',
  amount DECIMAL(10,2) COMMENT '订单金额',
  order_date DATE COMMENT '订单日期'
);`}
          rows={15}
          style={{ fontFamily: 'Consolas, Monaco, monospace', fontSize: 13 }}
        />
        <div style={{ marginTop: 12, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
          <h4 style={{ marginBottom: 8 }}>说明：</h4>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li>支持解析标准 SQL 的 CREATE TABLE 语句</li>
            <li>自动识别主键、外键、分区时间等语义类型</li>
            <li>自动提取字段注释作为描述</li>
            <li>数值类型字段默认为度量，其他类型默认为维度</li>
          </ul>
        </div>
      </Card>

      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={8}>
            <FormItem label="数据库连接（可选）">
              <Select
                showSearch
                placeholder="请选择数据库连接（可选）"
                loading={loading}
                allowClear
                onSelect={(value: number, option: any) => {
                  setSelectedDatabaseId(value);
                  const type = option?.type || '';
                  if (['STARROCKS', 'KYUUBI', 'PRESTO', 'TRINO'].includes(type)) {
                    queryCatalogList(value);
                  } else {
                    queryDbNameList(value, '');
                  }
                }}
                optionFilterProp="children"
              >
                {databaseList.map((item) => (
                  <Select.Option
                    key={item.id}
                    value={item.id}
                    type={item.type}
                    disabled={!item.hasUsePermission}
                  >
                    {item.name}
                  </Select.Option>
                ))}
              </Select>
            </FormItem>
          </Col>
          {catalogList.length > 0 && (
            <Col span={8}>
              <FormItem label="Catalog（可选）">
                <Select
                  showSearch
                  placeholder="请选择Catalog"
                  loading={loading}
                  allowClear
                  onSelect={handleCatalogSelect}
                  value={selectedCatalog || undefined}
                >
                  {catalogList.map((item) => (
                    <Select.Option key={item} value={item}>
                      {item}
                    </Select.Option>
                  ))}
                </Select>
              </FormItem>
            </Col>
          )}
          <Col span={8}>
            <FormItem label="数据库名（可选）">
              <Select
                showSearch
                placeholder="请选择数据库名"
                loading={loading}
                allowClear
                onSelect={handleDbNameSelect}
                value={selectedDbName || undefined}
                disabled={dbNameList.length === 0}
              >
                {dbNameList.map((item) => (
                  <Select.Option key={item} value={item}>
                    {item}
                  </Select.Option>
                ))}
              </Select>
            </FormItem>
          </Col>
        </Row>
      </Form>

      <div style={{ marginTop: 12, padding: 12, background: '#e6f7ff', borderRadius: 4 }}>
        <h4 style={{ marginBottom: 8 }}>提示：</h4>
        <p style={{ margin: 0 }}>
          数据库连接、Catalog、数据库名均为可选。如果选择了数据库连接，创建的模型将关联到该数据库。
        </p>
      </div>
    </div>
  );

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <div className={styles.batchImportStepContent}>
            {renderModeSelector()}
            {importMode === 'database' ? renderDatabaseStep() : renderDDLStep()}
          </div>
        );

      case 1:
        return (
          <div className={styles.batchImportStepContent}>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <div>
                  <Space>
                    <Switch
                      checked={useLLM}
                      onChange={(checked) => setUseLLM(checked)}
                    />
                    <span>使用LLM进行语义分析</span>
                    <Tooltip title="启用后将使用大模型自动分析表结构，生成语义类型、中文名称和描述">
                      <InfoCircleOutlined style={{ color: '#1890ff' }} />
                    </Tooltip>
                  </Space>
                </div>

                {useLLM && (
                  <div style={{ marginTop: 16 }}>
                    <Form layout="vertical">
                      <FormItem
                        label="选择LLM配置"
                        rules={[{ required: useLLM, message: '请选择LLM配置' }]}
                      >
                        <Select
                          showSearch
                          placeholder="请选择LLM配置"
                          value={selectedLlmId}
                          onChange={(value) => setSelectedLlmId(value)}
                          style={{ width: 400 }}
                        >
                          {llmList.map((item) => (
                            <Select.Option key={item.id} value={item.id}>
                              {item.name} ({item.config?.modelName || item.config?.provider})
                            </Select.Option>
                          ))}
                        </Select>
                      </FormItem>
                    </Form>

                    <div style={{ marginTop: 16, padding: 16, background: '#f5f5f5', borderRadius: 4 }}>
                      <h4 style={{ marginBottom: 8 }}>
                        已选择的{importMode === 'database' ? '表' : '内容'}：
                      </h4>
                      {importMode === 'database' ? (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                          {selectedTables.map((table) => (
                            <Tag key={table} color="blue">
                              {table}
                            </Tag>
                          ))}
                        </div>
                      ) : (
                        <div>
                          <Tag color="blue">
                            {ddlText.length > 100 ? ddlText.substring(0, 100) + '...' : ddlText || '未输入'}
                          </Tag>
                        </div>
                      )}
                    </div>

                    <div style={{ marginTop: 16, padding: 16, background: '#e6f7ff', borderRadius: 4 }}>
                      <h4 style={{ marginBottom: 8 }}>LLM语义分析说明：</h4>
                      <ul style={{ margin: 0, paddingLeft: 20 }}>
                        <li>自动识别字段的语义类型：主键、外键、分区时间、维度、度量</li>
                        <li>为度量字段自动推荐聚合函数（SUM, COUNT, AVG, MAX, MIN 等）</li>
                        <li>根据字段名和注释自动生成中文名称</li>
                        <li>分析表之间的关联关系，识别主键/外键关系</li>
                      </ul>
                    </div>
                  </div>
                )}

                {!useLLM && (
                  <div style={{ marginTop: 16, padding: 16, background: '#fff7e6', borderRadius: 4 }}>
                    <h4 style={{ marginBottom: 8 }}>不使用LLM：</h4>
                    <p style={{ margin: 0 }}>
                      将使用默认规则进行语义分析：数值类型默认为度量，其他类型默认为维度。
                      建议启用LLM以获得更智能的语义分析结果。
                    </p>
                  </div>
                )}
              </Space>
            </Card>
          </div>
        );

      case 2:
        return (
          <div className={styles.batchImportStepContent}>
            {Object.keys(modelSchemas).length > 0 ? (
              <div>
                <div style={{ marginBottom: 12 }}>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 16 }} />
                    <span style={{ fontWeight: 500 }}>
                      语义分析完成，共 {Object.keys(modelSchemas).length} 个模型
                    </span>
                  </Space>
                </div>

                <Tabs
                  activeKey={activeTabKey}
                  onChange={setActiveTabKey}
                  type="card"
                >
                  {Object.entries(modelSchemas).map(([tableName, schema]) => (
                    <TabPane
                      tab={
                        <Space>
                          <TableOutlined />
                          {tableName}
                          {schema.name && ` (${schema.name})`}
                        </Space>
                      }
                      key={tableName}
                    >
                      <Card size="small" style={{ marginBottom: 16 }}>
                        <Row gutter={24}>
                          <Col span={8}>
                            <div>
                              <span style={{ color: '#666' }}>模型中文名：</span>
                              <span style={{ fontWeight: 500 }}>{schema.name || '-'}</span>
                            </div>
                          </Col>
                          <Col span={8}>
                            <div>
                              <span style={{ color: '#666' }}>模型英文名：</span>
                              <span style={{ fontWeight: 500 }}>{schema.bizName || tableName}</span>
                            </div>
                          </Col>
                          <Col span={8}>
                            <div>
                              <span style={{ color: '#666' }}>字段数：</span>
                              <Tag color="blue">{schema.semanticColumns?.length || 0}</Tag>
                            </div>
                          </Col>
                        </Row>
                        {schema.description && (
                          <div style={{ marginTop: 12 }}>
                            <span style={{ color: '#666' }}>描述：</span>
                            <span>{schema.description}</span>
                          </div>
                        )}
                      </Card>

                      <div style={{ marginBottom: 8 }}>
                        <Space>
                          <span>字段统计：</span>
                          {schema.semanticColumns?.filter((c) => c.filedType === 'primary_key')
                            .length > 0 && (
                            <Tag color="purple">
                              主键:{' '}
                              {
                                schema.semanticColumns?.filter(
                                  (c) => c.filedType === 'primary_key',
                                ).length
                              }
                            </Tag>
                          )}
                          {schema.semanticColumns?.filter((c) => c.filedType === 'foreign_key')
                            .length > 0 && (
                            <Tag color="cyan">
                              外键:{' '}
                              {
                                schema.semanticColumns?.filter(
                                  (c) => c.filedType === 'foreign_key',
                                ).length
                              }
                            </Tag>
                          )}
                          {schema.semanticColumns?.filter((c) => c.filedType === 'partition_time')
                            .length > 0 && (
                            <Tag color="orange">
                              分区时间:{' '}
                              {
                                schema.semanticColumns?.filter(
                                  (c) => c.filedType === 'partition_time',
                                ).length
                              }
                            </Tag>
                          )}
                          {schema.semanticColumns?.filter((c) => c.filedType === 'time')
                            .length > 0 && (
                            <Tag color="gold">
                              时间:{' '}
                              {
                                schema.semanticColumns?.filter(
                                  (c) => c.filedType === 'time',
                                ).length
                              }
                            </Tag>
                          )}
                          <Tag color="blue">
                            维度:{' '}
                            {
                              schema.semanticColumns?.filter(
                                (c) => c.filedType === 'categorical',
                              ).length
                            }
                          </Tag>
                          <Tag color="green">
                            度量:{' '}
                            {
                              schema.semanticColumns?.filter((c) => c.filedType === 'measure')
                                .length
                            }
                          </Tag>
                        </Space>
                      </div>

                      <Table
                        columns={schemaColumns}
                        dataSource={getSchemaColumns(schema)}
                        pagination={false}
                        size="small"
                        scroll={{ y: 300 }}
                      />
                    </TabPane>
                  ))}
                </Tabs>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <EyeOutlined style={{ fontSize: 48, color: '#ccc' }} />
                <p style={{ marginTop: 16, color: '#666' }}>暂无预览数据，请先执行语义分析</p>
              </div>
            )}
          </div>
        );

      default:
        return null;
    }
  };

  const renderFooter = () => {
    const footerBtns: React.ReactNode[] = [
      <Button key="cancel" onClick={onCancel}>
        取消
      </Button>,
    ];

    if (currentStep > 0) {
      footerBtns.push(
        <Button key="prev" onClick={handlePrev}>
          上一步
        </Button>,
      );
    }

    if (currentStep < 2) {
      if (currentStep === 1) {
        const isDisabled = importMode === 'database' 
          ? selectedTables.length === 0 
          : !ddlText.trim();
        footerBtns.push(
          <Button
            key="analyze"
            type="primary"
            onClick={handleAnalyze}
            loading={analyzing}
            disabled={isDisabled}
          >
            {analyzing ? '语义分析中...' : '开始语义分析'}
          </Button>,
        );
      } else {
        const isDisabled = importMode === 'database'
          ? selectedTables.length === 0
          : !ddlText.trim();
        footerBtns.push(
          <Button
            key="next"
            type="primary"
            onClick={handleNext}
            disabled={isDisabled}
          >
            下一步
          </Button>,
        );
      }
    }

    if (currentStep === 2) {
      footerBtns.push(
        <Button
          key="create"
          type="primary"
          onClick={handleCreate}
          loading={creating}
          disabled={Object.keys(modelSchemas).length === 0}
        >
          {creating ? '创建中...' : '批量创建模型'}
        </Button>,
      );
    }

    return footerBtns;
  };

  return (
    <Modal
      title={
        <Space>
          <ImportOutlined />
          批量导入模型
        </Space>
      }
      open={open}
      onCancel={onCancel}
      width={1000}
      footer={renderFooter()}
      maskClosable={false}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <Steps current={currentStep} items={steps} style={{ marginBottom: 24 }} />
        {renderStepContent()}
      </Spin>
    </Modal>
  );
};

export default BatchImportModal;
