import React, { useState } from 'react';
import { Form, Button, Modal, Input, Table, message, Upload, Tabs, Tag } from 'antd';
import { PlusOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined, FileExcelOutlined } from '@ant-design/icons';
import { formLayout } from '@/components/FormHelper/utils';
import styles from '../style.less';
import { saveBatchTerm, uploadTerm, downloadTermTemplate } from '../../service';

export type BatchCreateFormProps = {
  visible: boolean;
  domainId: number;
  onCancel?: () => void;
  onSuccess?: () => void;
};

interface TermData {
  key: string;
  name: string;
  alias: string;
  description: string;
}

interface UploadResult {
  rowNum: number;
  name: string;
  alias: string;
  description: string;
  success: boolean;
  message: string;
}

const TermBatchCreateForm: React.FC<BatchCreateFormProps> = ({
  visible,
  domainId,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TermData[]>([
    {
      key: '0',
      name: '',
      alias: '',
      description: '',
    },
  ]);
  const [uploadResults, setUploadResults] = useState<UploadResult[]>([]);

  const handleAdd = () => {
    setDataSource([
      ...dataSource,
      {
        key: String(Date.now()),
        name: '',
        alias: '',
        description: '',
      },
    ]);
  };

  const handleDelete = (key: string) => {
    if (dataSource.length > 1) {
      setDataSource(dataSource.filter((item) => item.key !== key));
    }
  };

  const handleTableCellChange = (key: string, field: string, value: string) => {
    setDataSource(
      dataSource.map((item) => {
        if (item.key === key) {
          return { ...item, [field]: value };
        }
        return item;
      }),
    );
  };

  const validateData = () => {
    let isValid = true;
    dataSource.forEach((item) => {
      if (!item.name || !item.description) {
        isValid = false;
      }
    });
    return isValid;
  };

  const handleUpload = async (file: File) => {
    if (!file.name.match(/\.(xlsx|xls|csv)$/i)) {
      message.error('只支持 Excel (.xlsx, .xls) 和 CSV 文件');
      return false;
    }
    setUploadLoading(true);
    try {
      const res = await uploadTerm(file, domainId);
      if (res.code === 200 && res.data) {
        setUploadResults(res.data);
        const successCount = res.data.filter((r: UploadResult) => r.success).length;
        const failCount = res.data.length - successCount;
        if (failCount === 0) {
          message.success(`导入成功，共 ${successCount} 条术语`);
          onSuccess?.();
        } else {
          message.warning(`导入完成，成功 ${successCount} 条，失败 ${failCount} 条`);
        }
      } else {
        message.error(res.msg || '导入失败');
      }
    } catch (error) {
      message.error('导入失败');
    } finally {
      setUploadLoading(false);
    }
    return false;
  };

  const handleDownloadTemplate = () => {
    downloadTermTemplate();
  };

  const handleSubmit = async () => {
    if (!validateData()) {
      message.error('请填写完整的术语信息，名称和描述为必填项');
      return;
    }

    const validData = dataSource.filter((item) => item.name && item.description);
    if (validData.length === 0) {
      message.error('请至少填写一条术语信息');
      return;
    }

    setLoading(true);
    try {
      const termList = validData.map((item) => ({
        domainId,
        name: item.name,
        alias: item.alias ? item.alias.split(',') : [],
        description: item.description,
      }));

      const res = await saveBatchTerm(termList);
      if (res.code === 200) {
        message.success('批量创建成功');
        setDataSource([
          {
            key: '0',
            name: '',
            alias: '',
            description: '',
          },
        ]);
        onSuccess?.();
      } else {
        message.error(res.msg || '批量创建失败');
      }
    } catch (error) {
      message.error('批量创建失败');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '名称 *',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (text: string, record: TermData) => (
        <Input
          value={text}
          placeholder="请输入术语名称"
          onChange={(e) => handleTableCellChange(record.key, 'name', e.target.value)}
        />
      ),
    },
    {
      title: '近义词（逗号分隔）',
      dataIndex: 'alias',
      key: 'alias',
      width: 280,
      render: (text: string, record: TermData) => (
        <Input
          value={text}
          placeholder="多个近义词用逗号分隔"
          onChange={(e) => handleTableCellChange(record.key, 'alias', e.target.value)}
        />
      ),
    },
    {
      title: '描述 *',
      dataIndex: 'description',
      key: 'description',
      render: (text: string, record: TermData) => (
        <Input.TextArea
          value={text}
          placeholder="请输入术语描述"
          rows={2}
          onChange={(e) => handleTableCellChange(record.key, 'description', e.target.value)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: any, record: TermData) => (
        <Button
          type="link"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDelete(record.key)}
          disabled={dataSource.length <= 1}
        />
      ),
    },
  ];

  const uploadResultColumns = [
    {
      title: '行号',
      dataIndex: 'rowNum',
      key: 'rowNum',
      width: 80,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '近义词',
      dataIndex: 'alias',
      key: 'alias',
      width: 200,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'success',
      key: 'success',
      width: 100,
      render: (success: boolean) => (
        <Tag color={success ? 'green' : 'red'}>
          {success ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
  ];

  return (
    <Modal
      forceRender
      width={1100}
      style={{ top: 48 }}
      destroyOnClose
      title="批量创建术语"
      maskClosable={false}
      open={visible}
      footer={
        <div className={styles.modalFooter}>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={loading}>
            批量创建
          </Button>
        </div>
      }
      onCancel={onCancel}
    >
      <Tabs defaultActiveKey="manual">
        <Tabs.TabPane tab="手动录入" key="manual">
          <Form
            {...formLayout}
            form={form}
            initialValues={{
              dataSource,
            }}
            className={styles.form}
          >
            <div style={{ marginBottom: 16 }}>
              <Button type="dashed" onClick={handleAdd} icon={<PlusOutlined />} block>
                添加术语
              </Button>
            </div>
            <Table
              dataSource={dataSource}
              columns={columns}
              pagination={false}
              rowKey="key"
              scroll={{ y: 400 }}
              bordered
            />
          </Form>
        </Tabs.TabPane>
        <Tabs.TabPane tab="文件导入" key="upload">
          <div style={{ marginBottom: 20, padding: '20px', border: '1px dashed #d9d9d9', borderRadius: 8 }}>
            <div style={{ marginBottom: 16 }}>
              <Button
                type="link"
                icon={<DownloadOutlined />}
                onClick={handleDownloadTemplate}
              >
                下载导入模板
              </Button>
              <span style={{ marginLeft: 8, color: '#666' }}>
                支持 .xlsx, .xls, .csv 格式，需填写名称和描述字段
              </span>
            </div>
            <Upload
              beforeUpload={handleUpload}
              showUploadList={false}
              accept=".xlsx,.xls,.csv"
            >
              <Button icon={<UploadOutlined />} loading={uploadLoading} type="primary">
                选择文件上传
              </Button>
            </Upload>
          </div>
          {uploadResults.length > 0 && (
            <div>
              <div style={{ marginBottom: 8 }}>
                <FileExcelOutlined style={{ marginRight: 8 }} />
                导入结果：共 {uploadResults.length} 条
              </div>
              <Table
                dataSource={uploadResults}
                columns={uploadResultColumns}
                pagination={false}
                rowKey="rowNum"
                scroll={{ y: 300 }}
                bordered
                size="small"
              />
            </div>
          )}
        </Tabs.TabPane>
      </Tabs>
    </Modal>
  );
};

export default TermBatchCreateForm;
