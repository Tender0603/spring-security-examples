-- Run manually in SQL Server Management Studio using an account with CREATE DATABASE permission.
-- This script only creates webst10 when missing; it never modifies webst8/webst9.
USE master;
GO
IF DB_ID(N'webst10') IS NULL
    EXEC(N'CREATE DATABASE [webst10]');
GO
