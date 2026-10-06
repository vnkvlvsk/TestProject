CREATE USER mcp_readonly WITH PASSWORD 'mcp_readonly';
GRANT CONNECT ON DATABASE limit_service TO mcp_readonly;
GRANT USAGE ON SCHEMA public TO mcp_readonly;
ALTER DEFAULT PRIVILEGES FOR USER limit_service IN SCHEMA public GRANT SELECT ON TABLES TO mcp_readonly;
