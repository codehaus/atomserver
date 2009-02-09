if exists (select 1
            from  sysindexes
           where  id    = object_id('VW_EntryWithCategory')
            and   name  = 'IX2_VW_EntryWithCategory'
            and   indid > 0
            and   indid < 255)
   drop index VW_EntryWithCategory.IX2_VW_EntryWithCategory
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('VW_EntryWithCategory')
            and   name  = 'IX1_VW_EntryWithCategory'
            and   indid > 0
            and   indid < 255)
   drop index VW_EntryWithCategory.IX1_VW_EntryWithCategory
go

if exists (select 1
            from  sysobjects
           where  id = object_id('VW_EntryWithCategory')
            and   type = 'V')
   drop view VW_EntryWithCategory
go

CREATE VIEW VW_EntryWithCategory WITH SCHEMABINDING AS
SELECT entries.EntryStoreId,
       entries.UpdateTimestamp,
       entries.LanCode,
       entries.CountryCode,
       entries.Workspace,
       categories.Scheme,
       categories.Term
  FROM dbo.EntryStore entries
  JOIN dbo.EntryCategory categories ON entries.EntryStoreId = categories.EntryStoreId
GO

CREATE UNIQUE CLUSTERED INDEX IX1_VW_EntryWithCategory ON VW_EntryWithCategory
(Scheme ASC, UpdateTimestamp ASC, Term ASC)
GO

CREATE NONCLUSTERED INDEX IX2_VW_EntryWithCategory ON VW_EntryWithCategory
(Scheme ASC, Term ASC, LanCode ASC, CountryCode ASC, Workspace ASC)
INCLUDE (UpdateTimestamp, EntryStoreId)
GO

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX6_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX6_EntryStore
go

CREATE UNIQUE NONCLUSTERED INDEX [IX6_EntryStore] ON [dbo].[EntryStore]
(
        [Workspace] ASC,
        [Collection] ASC,
        [UpdateTimestamp] ASC
)
GO

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX2_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX2_EntryStore
go

grant select on VW_EntryWithCategory to HAData
