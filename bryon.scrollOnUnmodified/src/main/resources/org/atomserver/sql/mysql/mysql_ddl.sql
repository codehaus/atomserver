--
-- Table structure for testing rentals table using MySQL 
--

DROP TABLE IF EXISTS EntryContent;
DROP TABLE IF EXISTS EntryCategory;
DROP TABLE IF EXISTS EntryStore;
DROP TABLE IF EXISTS AtomCollection;
DROP TABLE IF EXISTS AtomWorkspace;


CREATE TABLE AtomWorkspace (
Workspace           VARCHAR(20)             NOT NULL,
SeqNum              BIGINT                  NOT NULL,
PRIMARY KEY (Workspace)
) ENGINE=InnoDB;

CREATE TABLE AtomCollection (
Workspace           VARCHAR(20)             NOT NULL,
Collection          VARCHAR(20)             NOT NULL,
PRIMARY KEY (Workspace, Collection),
FOREIGN KEY (Workspace) REFERENCES AtomWorkspace(Workspace)
) ENGINE=InnoDB;

CREATE TABLE EntryStore (
EntryStoreId        BIGINT                  NOT NULL AUTO_INCREMENT,
Workspace           VARCHAR(20)             NOT NULL,
Collection          VARCHAR(20)             NOT NULL,
EntryId             VARCHAR(32)             NOT NULL,
LanCode             CHAR(2)                 NOT NULL DEFAULT '**',
CountryCode         CHAR(2)                 NOT NULL DEFAULT '**',
UpdateDate          DATETIME                NOT NULL,
CreateDate          DATETIME                NOT NULL,
UpdateSeqNum        BIGINT                  NOT NULL,
DeleteFlag          TINYINT(1) UNSIGNED     NOT NULL,
RevisionNum         INT UNSIGNED            NOT NULL,
PRIMARY KEY (EntryStoreId),
UNIQUE (UpdateSeqNum, Workspace),
UNIQUE INDEX (Workspace, Collection, EntryId, LanCode, CountryCode),
FOREIGN KEY (Workspace, Collection) REFERENCES AtomCollection(Workspace, Collection)
) ENGINE=InnoDB;

CREATE TABLE EntryCategory (
EntryCategoryId     BIGINT                  NOT NULL AUTO_INCREMENT,
EntryStoreId        BIGINT                  NOT NULL,
Scheme              VARCHAR(128)            NOT NULL,
Term                VARCHAR(32)             NOT NULL,
Label               VARCHAR(128)            ,
PRIMARY KEY (EntryCategoryId),
INDEX complete_INDEX (EntryStoreId, Scheme, Term),
INDEX cat_INDEX (Scheme, Term),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
) ENGINE=InnoDB;

CREATE TABLE EntryContent (
EntryStoreId        BIGINT                  NOT NULL,
Content             LONGTEXT                ,
PRIMARY KEY (EntryStoreId),
FOREIGN KEY (EntryStoreId) REFERENCES EntryStore(EntryStoreId)
) ENGINE=InnoDB;

