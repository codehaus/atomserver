/*==============================================================*/
/* DBMS name:      Microsoft SQL Server 2000                    */
/* Created on:     3/4/2008 7:33:20 PM                          */
/*==============================================================*/


alter table AtomCollection
   drop constraint FK1_AtomCollection
go

alter table EntryCategory
   drop constraint FK1_EntryCategory
go

alter table EntryContent
   drop constraint FK1_EntryContent
go

alter table EntryStore
   drop constraint FK1_EntryStore
go

alter table EntryStore
   drop constraint FK2_EntryStore
go

alter table EntryStore
   drop constraint FK3_EntryStore
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
            and   name  = 'IX5_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX5_EntryStore
go

alter table AtomCollection
   drop constraint PK_AtomCollection
go

alter table AtomWorkspace
   drop constraint PK_AtomWorkspace
go

alter table Country
   drop constraint PK_Country
go

alter table EntryCategory
   drop constraint PK_EntryCategory
go

alter table EntryContent
   drop constraint PK_EntryContent
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
           where  id = object_id('AtomCollection')
            and   type = 'U')
   drop table AtomCollection
go

if exists (select 1
            from  sysobjects
           where  id = object_id('AtomWorkspace')
            and   type = 'U')
   drop table AtomWorkspace
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
           where  id = object_id('EntryContent')
            and   type = 'U')
   drop table EntryContent
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

/*==============================================================*/
/* Table: AtomCollection                                        */
/*==============================================================*/
create table AtomCollection (
   Workspace            varchar(20)          not null,
   Collection           varchar(20)          not null
)
go

alter table AtomCollection
   add constraint PK_AtomCollection primary key  (Workspace, Collection)
go

/*==============================================================*/
/* Table: AtomWorkspace                                         */
/*==============================================================*/
create table AtomWorkspace (
   Workspace            varchar(20)          not null,
   SeqNum               bigint               not null
)
go

alter table AtomWorkspace
   add constraint PK_AtomWorkspace primary key  (Workspace)
go

/*==============================================================*/
/* Table: Country                                               */
/*==============================================================*/
create table Country (
   CountryCode          char(2)              not null,
   CountryName          varchar(50)          not null
)
go

alter table Country
   add constraint PK_Country primary key  (CountryCode)
go

/*==============================================================*/
/* Table: EntryCategory                                         */
/*==============================================================*/
create table EntryCategory (
   EntryCategoryId      int                  identity,
   EntryStoreId         int                  not null,
   Scheme               varchar(128)         not null,
   Term                 varchar(32)          not null,
   Label                varchar(128)         null
)
go

alter table EntryCategory
   add constraint PK_EntryCategory primary key  (EntryCategoryId)
go

/*==============================================================*/
/* Index: IX1_EntryCategory                                     */
/*==============================================================*/
create unique  index IX1_EntryCategory on EntryCategory (
EntryStoreId ASC,
Scheme ASC,
Term ASC
)
go

/*==============================================================*/
/* Table: EntryContent                                          */
/*==============================================================*/
create table EntryContent (
   EntryStoreId         int                  not null,
   Content              varchar(max)         not null
)
go

alter table EntryContent
   add constraint PK_EntryContent primary key  (EntryStoreId)
go

/*==============================================================*/
/* Table: EntryStore                                            */
/*==============================================================*/
create table EntryStore (
   EntryStoreId         int                  identity,
   Workspace            varchar(20)          not null,
   Collection           varchar(20)          not null,
   EntryId              varchar(32)          not null,
   LanCode              char(2)              not null,
   CountryCode          char(2)              not null,
   CreateDate           datetime             not null,
   UpdateDate           datetime             not null,
   UpdateSeqNum         bigint               not null,
   DeleteFlag           bit                  not null,
   RevisionNum          int                  not null,
   UpdateTimestamp      timestamp            not null
)
go

alter table EntryStore
   add constraint PK_EntryStore primary key  (EntryStoreId)
go

/*==============================================================*/
/* Index: IX1_EntryStore                                        */
/*==============================================================*/
create   index IX1_EntryStore on EntryStore (
LanCode ASC
)
go

/*==============================================================*/
/* Index: IX2_EntryStore                                        */
/*==============================================================*/
create   index IX2_EntryStore on EntryStore (
CountryCode ASC
)
go

/*==============================================================*/
/* Index: IX3_EntryStore                                        */
/*==============================================================*/
create   index IX3_EntryStore on EntryStore (
UpdateDate ASC
)
go

/*==============================================================*/
/* Index: IX5_EntryStore                                        */
/*==============================================================*/
create unique  index IX5_EntryStore on EntryStore (
Workspace ASC,
Collection ASC,
EntryId ASC,
LanCode ASC,
CountryCode ASC
)
go

/*==============================================================*/
/* Table: Language                                              */
/*==============================================================*/
create table Language (
   LanCode              char(2)              not null,
   LanName              varchar(50)          not null
)
go

alter table Language
   add constraint PK_language primary key  (LanCode)
go

/*==============================================================*/
/* Table: WorkspaceSequence                                     */
/*==============================================================*/
create table WorkspaceSequence (
   Workspace            varchar(20)          not null,
   SeqNum               bigint               not null
)
go

alter table WorkspaceSequence
   add constraint PK_WorkspaceSequence primary key  (Workspace)
go

alter table AtomCollection
   add constraint FK1_AtomCollection foreign key (Workspace)
      references AtomWorkspace (Workspace)
go

alter table EntryCategory
   add constraint FK1_EntryCategory foreign key (EntryStoreId)
      references EntryStore (EntryStoreId)
go

alter table EntryContent
   add constraint FK1_EntryContent foreign key (EntryStoreId)
      references EntryStore (EntryStoreId)
go

alter table EntryStore
   add constraint FK1_EntryStore foreign key (LanCode)
      references Language (LanCode)
go

alter table EntryStore
   add constraint FK2_EntryStore foreign key (CountryCode)
      references Country (CountryCode)
go

alter table EntryStore
   add constraint FK3_EntryStore foreign key (Workspace, Collection)
      references AtomCollection (Workspace, Collection)
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('VW_AggregateEntry')
            and   name  = 'IX2_VW_AggregateEntry'
            and   indid > 0
            and   indid < 255)
   drop index VW_AggregateEntry.IX2_VW_AggregateEntry
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('VW_AggregateEntry')
            and   name  = 'IX1_VW_AggregateEntry'
            and   indid > 0
            and   indid < 255)
   drop index VW_AggregateEntry.IX1_VW_AggregateEntry
go

if exists (select 1
            from  sysobjects
           where  id = object_id('VW_AggregateEntry')
            and   type = 'V')
   drop view VW_AggregateEntry
go

/*==============================================================*/
/* View: VW_AggregateEntry                                      */
/*==============================================================*/
create view VW_AggregateEntry with SchemaBinding as
SELECT
       joincat.Scheme AS Collection,
       joincat.Term AS EntryId,
		entries.[LanCode],
		entries.[CountryCode],
       (entries.UpdateTimestamp) AS UpdateTimestamp,
       (entries.UpdateDate) AS UpdateDate,
       (entries.CreateDate) AS CreateDate
  FROM dbo.EntryCategory joincat
  JOIN dbo.EntryStore entries
    ON joincat.EntryStoreId = entries.EntryStoreId
go

/*==============================================================*/
/* Index: IX1_VW_AggregateEntry                                 */
/*==============================================================*/
create unique clustered index IX1_VW_AggregateEntry on VW_AggregateEntry (
Collection ASC,
UpdateTimestamp DESC,
EntryId ASC
)
go

create index IX2_VW_AggregateEntry on VW_AggregateEntry (
LanCode ASC,
CountryCode ASC,
Collection ASC,
EntryId ASC
)
go

grant select on VW_AggregateEntry to HA_Data