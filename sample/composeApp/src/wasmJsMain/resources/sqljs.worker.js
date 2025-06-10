import initSqlJs from 'sql.js';

let dbPromise;

async function getDatabase() {
  if (!dbPromise) {
    const SQL = await initSqlJs({ locateFile: file => '/sql-wasm.wasm' });
    const response = await fetch('https://github.com/kdroidFilter/KDroidDatabase/releases/latest/download/store-database.db');
    const buffer = await response.arrayBuffer();
    dbPromise = new SQL.Database(new Uint8Array(buffer));
  }
  return dbPromise;
}

self.onmessage = async (event) => {
  const data = event.data;
  try {
    const db = await getDatabase();
    switch (data && data.action) {
      case 'exec':
        if (!data['sql']) throw new Error('exec: Missing query string');
        postMessage({ id: data.id, results: db.exec(data.sql, data.params)[0] ?? { values: [] } });
        break;
      case 'begin_transaction':
        postMessage({ id: data.id, results: db.exec('BEGIN TRANSACTION;') });
        break;
      case 'end_transaction':
        postMessage({ id: data.id, results: db.exec('END TRANSACTION;') });
        break;
      case 'rollback_transaction':
        postMessage({ id: data.id, results: db.exec('ROLLBACK TRANSACTION;') });
        break;
      default:
        throw new Error(`Unsupported action: ${data && data.action}`);
    }
  } catch (err) {
    postMessage({ id: data.id, error: err });
  }
};
