if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryStore')
            and   name  = 'IX1_EntryStore'
            and   indid > 0
            and   indid < 255)
   drop index EntryStore.IX1_EntryStore
go
create   index IX1_EntryStore on EntryStore (
LanCode ASC,
CountryCode ASC,
UpdateTimestamp ASC
)
go


if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryCategory')
            and   name  = 'IX1_EntryCategory'
            and   indid > 0
            and   indid < 255)
   drop index EntryCategory.IX1_EntryCategory
go

/*==============================================================*/
/* Index: IX1_EntryCategory                                     */
/*==============================================================*/
create unique  index IX1_EntryCategory on EntryCategory (
EntryStoreId ASC,
Scheme ASC,
Term ASC,
Label ASC
)
go

if exists (select 1
            from  sysindexes
           where  id    = object_id('EntryCategory')
            and   name  = 'IX2_EntryCategory'
            and   indid > 0
            and   indid < 255)
   drop index EntryCategory.IX2_EntryCategory
go

/*==============================================================*/
/* Index: IX2_EntryCategory                                     */
/*==============================================================*/
create unique  index IX2_EntryCategory on EntryCategory (
Scheme ASC,
Term ASC,
EntryStoreId ASC
)
go