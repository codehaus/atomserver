
DROP TABLE IF EXISTS EntryContent CASCADE;
DROP TABLE IF EXISTS EntryCategoryLogEvent CASCADE;
DROP TABLE IF EXISTS EntryCategory CASCADE;
DROP TABLE IF EXISTS EntryStore CASCADE;
DROP TABLE IF EXISTS AtomCollection CASCADE;
DROP TABLE IF EXISTS AtomWorkspace CASCADE;
DROP VIEW IF EXISTS vw_AggregateEntry;
DROP SEQUENCE entrystore_updatetimestamp_seq IF EXISTS; 

/*==============================================================*/
/* Table: AtomWorkspace                                         */
/*==============================================================*/
CREATE TABLE AtomWorkspace (
Workspace           VARCHAR(20)             NOT NULL,
SeqNum              BIGINT                  NOT NULL,
PRIMARY KEY (Workspace)
);

/*==============================================================*/
/* Table: AtomCollection                                        */
/*==============================================================*/
CREATE TABLE AtomCollection (
Workspace           VARCHAR(20)             NOT NULL,
Collection          VARCHAR(20)             NOT NULL,
PRIMARY KEY (Workspace, Collection),
FOREIGN KEY (Workspace) REFERENCES AtomWorkspace(Workspace)
);

CREATE SEQUENCE entrystore_updatetimestamp_seq START WITH 1;

/*==============================================================*/
/* Table: EntryStore                                            */
/*==============================================================*/

CREATE TABLE EntryStore (
EntryStoreId        BIGINT                  IDENTITY,
Workspace           VARCHAR(20)             NOT NULL,
Collection          VARCHAR(20)             NOT NULL,
EntryId             VARCHAR(32)             NOT NULL,
LanCode             CHAR(2)                 DEFAULT '**' NOT NULL,
CountryCode         CHAR(2)                 DEFAULT '**' NOT NULL,
UpdateDate          TIMESTAMP               NOT NULL,
CreateDate          TIMESTAMP               NOT NULL,
DeleteFlag          BOOLEAN,
RevisionNum         INT                     NOT NULL,
UpdateSeqNum        BIGINT                  NOT NULL,
UpdateTimestamp     BIGINT                  NOT NULL,
PRIMARY KEY (EntryStoreId),
UNIQUE (UpdateTimestamp, Workspace),
UNIQUE (Workspace, Collection, EntryId, LanCode, CountryCode),
FOREIGN KEY (Workspace, Collection) REFERENCES AtomCollection(Workspace, Collection)
);

CREATE INDEX IX1_EntryStore ON EntryStore (LanCode);
CREATE INDEX IX2_EntryStore ON EntryStore (CountryCode);
CREATE INDEX IX3_EntryStore on EntryStore (UpdateDate);

/*==============================================================*/
/* Table: EntryCategory                                         */
/*==============================================================*/
CREATE TABLE EntryCategory (
EntryCategoryId     BIGINT                  IDENTITY,
EntryStoreId        BIGINT                  NOT NULL,
Scheme              VARCHAR(128)            NOT NULL,
Term                VARCHAR(32)             NOT NULL,
Label               VARCHAR(128)            NULL,
PRIMARY KEY (EntryCategoryId),
UNIQUE (EntryStoreId, Scheme, Term),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
);

/*==============================================================*/
/* Table: EntryContent                                          */
/*==============================================================*/
CREATE TABLE EntryContent (
EntryStoreId        BIGINT                  NOT NULL,
Content             VARCHAR                 NOT NULL,
PRIMARY KEY (EntryStoreId),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
);

/*==============================================================*/
/* Table: EntryCategoryLogEvent                                 */
/*==============================================================*/
CREATE TABLE EntryCategoryLogEvent (
EntryCategoryLogEventId  BIGINT                  IDENTITY,
EntryStoreId             BIGINT                  NOT NULL,
Scheme                   VARCHAR(128)            NOT NULL,
Term                     VARCHAR(32)             NOT NULL,
Label                    VARCHAR(128)            NULL,
CreateDate               TIMESTAMP               NOT NULL,
PRIMARY KEY (EntryCategoryLogEventId),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
);


/*==============================================================*/
/* View: vw_EntryWithCategory                                   */
/* NOTE: in SQL Server, we get a significant performance boost  */
/*       by indexing this view -- since HSQLDB does not         */
/*       support indexed or materialized views, this view does  */
/*       not provide any real performance boost, but it is      */
/*       created to keep the queries for aggregate feeds as     */
/*       similar as possible.                                   */
/*==============================================================*/
CREATE VIEW vw_EntryWithCategory AS
    SELECT entries.EntryStoreId,
           entries.UpdateTimestamp,
           entries.LanCode,
           entries.CountryCode,
           entries.Workspace,
           entries.UpdateDate,
           categories.Scheme,
           categories.Term
      FROM EntryStore entries
      JOIN EntryCategory categories
        ON entries.EntryStoreId = categories.EntryStoreId
