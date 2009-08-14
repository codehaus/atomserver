alter table entryfile drop constraint FK1_EntryFile
go
alter table entryfile drop constraint pk_entryfile
go
drop table entryfile
go

alter table EntryStore   drop constraint PK_EntryStore
go
alter table EntryStore add   EntryStoreId      int                  identity
GO
alter table EntryStore
   add constraint PK_EntryStore primary key  (EntryStoreId)
go

create unique  index IX5_EntryStore on EntryStore (Workspace, Collection, EntryId, LanCode, CountryCode)
go

drop index entrystore.ix4_entrystore
go
create unique  index IX4_EntryStore on EntryStore (
UpdateSeqNum ASC,
Workspace ASC
)
go
-----------------------------------------------------------------

alter table EntryCategory drop constraint pk_EntryCategory
GO
drop index EntryCategory.IX2_EntryCategory
go
drop index EntryCategory.IX1_EntryCategory
go

create table NewCategory (
   EntryCategoryId      int                  identity,
   EntryStoreId         int                  not null,
   Scheme               varchar(128)         not null,
   Term                 varchar(32)          not null,
   Label                varchar(128)         null
)
go

alter table NewCategory
   add constraint PK_EntryCategory primary key  (EntryCategoryId)
go

create unique  index IX1_EntryCategory on NewCategory (
EntryStoreId ASC,
Scheme ASC,
Term ASC
)
go

grant SELECT,INSERT,DELETE,UPDATE on NewCategory to HA_Data
go

alter table NewCategory
   add constraint FK1_EntryCategory foreign key (EntryStoreId)
      references EntryStore (EntryStoreId)
go


--------------------------------------------------------------
INSERT INTO NewCategory (EntryStoreId, Scheme, Term, Label)
SELECT EntryStoreId, Scheme, Term, Label 
  FROM EntryStore s JOIN EntryCategory c
    ON s.Workspace = c.Workspace AND s.Collection = c.Collection and s.EntryId = c.EntryId
GO

exec sp_rename 'EntryCategory', 'OldCategory'
exec sp_rename 'NewCategory', 'EntryCategory'
GO
