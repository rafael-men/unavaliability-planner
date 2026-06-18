package com.unavaliability.backend.domain;


public final class Status {

    private Status() {
    }

    public static final class Unavailability {
        public static final String PENDING = "pending";
        public static final String APPROVED = "approved";
        public static final String REJECTED = "rejected";
        public static final String CANCELED = "canceled";

        private Unavailability() {
        }
    }

    public static final class UserAccount {
        public static final String PENDING = "pending";
        public static final String APPROVED = "approved";
        public static final String REJECTED = "rejected";

        private UserAccount() {
        }
    }

    public static final class Ticket {
        public static final String OPEN = "open";
        public static final String RESOLVED = "resolved";

        private Ticket() {
        }
    }

    public static final class AuditAction {
        public static final String CREATED = "created";
        public static final String UPDATED = "updated";
        public static final String APPROVED = "approved";
        public static final String REJECTED = "rejected";
        public static final String CANCELED = "canceled";
        public static final String SHORTENED = "shortened";

        private AuditAction() {
        }
    }
}
