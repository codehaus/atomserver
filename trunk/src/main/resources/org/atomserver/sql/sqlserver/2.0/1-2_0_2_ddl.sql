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