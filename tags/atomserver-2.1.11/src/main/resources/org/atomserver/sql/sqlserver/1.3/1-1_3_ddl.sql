
IF NOT EXISTS (SELECT name FROM sysobjects WHERE name = 'AtomWorkspace' AND type = 'U')
BEGIN
	PRINT 'creating AtomWorkspace table'
		create table AtomWorkspace (
		   Workspace            varchar(20)          not null,
		   SeqNum               bigint               not null  )

	alter table AtomWorkspace  add constraint PK_AtomWorkspace primary key  (Workspace)
END
GO

IF NOT EXISTS (SELECT name FROM sysobjects WHERE name = 'AtomCollection' AND type = 'U')
BEGIN
	PRINT 'creating AtomCollection table'
	create table AtomCollection (
	   Workspace            varchar(20)          not null,
	   Collection           varchar(20)          not null	)

	alter table AtomCollection	add constraint PK_AtomCollection primary key  (Workspace, Collection)
	alter table AtomCollection
	   add constraint FK1_AtomCollection foreign key (Workspace)
		  references AtomWorkspace (Workspace)
END
GO


IF NOT EXISTS (SELECT name FROM sysobjects WHERE name = 'EntryContent' AND type = 'U')
BEGIN
	PRINT 'creating EntryContent table'
	create table EntryContent (
	   EntryStoreId         int                  not null,
	   Content              varchar(max)         not null  )

	alter table EntryContent  add constraint PK_EntryContent primary key  (EntryStoreId)
	alter table EntryContent
	   add constraint FK1_EntryContent foreign key (EntryStoreId)
		  references EntryStore (EntryStoreId)

END
GO

IF (SELECT count(*) FROM AtomWorkspace) = 0
BEGIN
	insert into AtomWorkspace (workspace,seqnum)
	select workspace,seqnum from workspacesequence
END
GO

IF (SELECT count(*) FROM AtomCollection) = 0
BEGIN
	insert into AtomCollection (workspace, collection)
	select distinct workspace, collection 
	from EntryStore
END
GO


IF NOT EXISTS (SELECT name FROM sysobjects WHERE name = 'FK3_EntryStore' AND type = 'F')
BEGIN
	alter table EntryStore
		add constraint FK3_EntryStore foreign key (Workspace, Collection)
		references AtomCollection (Workspace, Collection)
END
go

IF NOT EXISTS (SELECT sc.name FROM syscolumns sc INNER JOIN sysobjects so ON sc.id = so.id WHERE so.name = 'EntryStore' AND so.type = 'U' AND sc.name = 'UpdateTimestamp')
BEGIN
	alter table EntryStore add   UpdateTimestamp      timestamp            not null
END
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX4_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX4_EntryStore
go


grant select,insert,update,delete on AtomWorkspace to HA_Data
grant select,insert,update,delete on AtomCollection to HA_Data
grant select,insert,update,delete on EntryContent to HA_Data