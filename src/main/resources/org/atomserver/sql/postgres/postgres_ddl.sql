
DROP TABLE IF EXISTS EntryContent CASCADE;
DROP TABLE IF EXISTS EntryCategory CASCADE;
DROP TABLE IF EXISTS EntryStore CASCADE;
DROP TABLE IF EXISTS AtomCollection CASCADE;
DROP TABLE IF EXISTS AtomWorkspace CASCADE;

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

/*==============================================================*/
/* Table: EntryStore                                            */
/*==============================================================*/

CREATE SEQUENCE entrystore_entrystoreid_seq;

CREATE TABLE EntryStore (
EntryStoreId        INT                     NOT NULL,
Workspace           VARCHAR(20)             NOT NULL,
Collection          VARCHAR(20)             NOT NULL,
EntryId             VARCHAR(32)             NOT NULL,
LanCode             CHAR(2)                 NOT NULL DEFAULT '**',
CountryCode         CHAR(2)                 NOT NULL DEFAULT '**',
UpdateDate          TIMESTAMP               NOT NULL,
CreateDate          TIMESTAMP               NOT NULL,
DeleteFlag          BOOLEAN,
RevisionNum         INT                     NOT NULL,
UpdateSeqNum        BIGINT                  NOT NULL,
UpdateTimestamp     SERIAL,
PRIMARY KEY (EntryStoreId),
UNIQUE (UpdateTimestamp, Workspace),
UNIQUE (Workspace, Collection, EntryId, LanCode, CountryCode),
FOREIGN KEY (Workspace, Collection) REFERENCES AtomCollection(Workspace, Collection)
);

CREATE INDEX IX1_EntryStore ON EntryStore (LanCode);
CREATE INDEX IX2_EntryStore ON EntryStore (CountryCode);
CREATE INDEX IX3_EntryStore on EntryStore (UpdateDate);

ALTER SEQUENCE entrystore_entrystoreid_seq OWNED BY EntryStore.EntryStoreId;

/*==============================================================*/
/* Table: EntryCategory                                         */
/*==============================================================*/
CREATE TABLE EntryCategory (
EntryCategoryId     SERIAL,
EntryStoreId        INT                     NOT NULL,
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
EntryStoreId        INT                     NOT NULL,
Content             TEXT                    NOT NULL,
PRIMARY KEY (EntryStoreId),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
);

