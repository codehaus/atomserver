use master
GO
IF not exists (SELECT 1 FROM sys.syslogins WHERE name = 'HAData')
BEGIN
	CREATE LOGIN HAData WITH PASSWORD=N'hadata', DEFAULT_DATABASE=HA_HUB
	, DEFAULT_LANGUAGE=[us_english], CHECK_EXPIRATION=OFF, CHECK_POLICY=OFF
END
GO

use HA_HUB
GO
CREATE ROLE HA_Data

CREATE USER HAData FOR LOGIN HAData
EXEC sp_addrolemember N'HA_Data', N'HAData'
GO

alter table EntryStore
   drop constraint FK1_EntryStore
go

alter table EntryStore
   drop constraint FK2_EntryStore
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryCategory')
            and   name  = 'IX1_EntryCategory'
            and   indid > 0
            and   indid < 255)
   drop index EntryCategory.IX1_EntryCategory
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX1_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX1_EntryStore
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX2_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX2_EntryStore
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX3_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX3_EntryStore
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX4_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX4_EntryStore
go

alter table Country
   drop constraint PK_Country
go

alter table EntryCategory
   drop constraint PK_EntryCategory
go

alter table EntryStore
   drop constraint PK_EntryStore
go

alter table Language
   drop constraint PK_language
go

alter table WorkspaceSequence
   drop constraint PK_WorkspaceSequence
go

if exists (select 1
            from  sysobjects
           where  id = object_id('Country')
            and   type = 'U')
   drop table Country
go

if exists (select 1
            from  sysobjects
           where  id = object_id('EntryCategory')
            and   type = 'U')
   drop table EntryCategory
go

if exists (select 1
            from  sysobjects
           where  id = object_id('EntryStore')
            and   type = 'U')
   drop table EntryStore
go

if exists (select 1
            from  sysobjects
           where  id = object_id('Language')
            and   type = 'U')
   drop table Language
go

if exists (select 1
            from  sysobjects
           where  id = object_id('WorkspaceSequence')
            and   type = 'U')
   drop table WorkspaceSequence
go

create table Country (
   CountryCode          char(2)              not null,
   CountryName          varchar(50)          not null
)
go

alter table Country
   add constraint PK_Country primary key  (CountryCode)
go

grant SELECT on Country to HA_Data
go

create table EntryCategory (
   Workspace            varchar(20)          not null,
   Collection           varchar(20)          not null,
   EntryId              varchar(32)          not null,
   Scheme               varchar(128)         not null,
   Term                 varchar(32)          not null,
   Label                varchar(128)         null
)
go

alter table EntryCategory
   add constraint PK_EntryCategory primary key  (Workspace, Collection, EntryId, Scheme, Term)
go

create   index IX1_EntryCategory on EntryCategory (
Scheme ASC,
Term ASC
)
go

grant SELECT,INSERT,DELETE,UPDATE on EntryCategory to HA_Data
go

create table EntryStore (
   Workspace            varchar(20)          not null,
   Collection           varchar(20)          not null,
   EntryId              varchar(32)          not null,
   LanCode              char(2)              not null,
   CountryCode          char(2)              not null,
   CreateDate           datetime             not null,
   UpdateDate           datetime             not null,
   UpdateSeqNum         bigint               not null,
   DeleteFlag           bit                  not null,
   RevisionNum          int                  not null
)
go

alter table EntryStore
   add constraint PK_EntryStore primary key  (Workspace, Collection, EntryId, LanCode, CountryCode)
go

create   index IX1_EntryStore on EntryStore (
LanCode ASC
)
go

create   index IX2_EntryStore on EntryStore (
CountryCode ASC
)
go

create   index IX3_EntryStore on EntryStore (
UpdateDate ASC
)
go

create   index IX4_EntryStore on EntryStore (
UpdateSeqNum ASC
)
go

grant SELECT,INSERT,DELETE,UPDATE on EntryStore to HA_Data
go

create table Language (
   LanCode              char(2)              not null,
   LanName              varchar(50)          not null
)
go

alter table Language
   add constraint PK_language primary key  (LanCode)
go

grant SELECT on Language to HA_Data
go

create table WorkspaceSequence (
   Workspace            varchar(20)          not null,
   SeqNum               bigint               not null
)
go

alter table WorkspaceSequence
   add constraint PK_WorkspaceSequence primary key  (Workspace)
go

grant SELECT,INSERT,DELETE,UPDATE on WorkspaceSequence to HA_Data
go

alter table EntryStore
   add constraint FK1_EntryStore foreign key (LanCode)
      references Language (LanCode)
go

alter table EntryStore
   add constraint FK2_EntryStore foreign key (CountryCode)
      references Country (CountryCode)
go

