package com.xuesinuo.pignoo.test.autodatabase.test02entity.childpackage;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

@Table
@Data
public class AutodatabaseTest02Child {
    private Long id;
    private String name;

    @Data
    public static class AutodatabaseTest02ChildPublicStatic {
        private Long id;
        private String name;
    }

    @Data
    static class AutodatabaseTest02ChildStatic {
        private Long id;
        private String name;
    }

    @Data
    protected static class AutodatabaseTest02ChildProtectedStatic {
        private Long id;
        private String name;
    }

    @Data
    private static class AutodatabaseTest02ChildPrivateStatic {
        private Long id;
        private String name;
    }

    @Data
    public class AutodatabaseTest02ChildPublic {
        private Long id;
        private String name;
    }

    @Data
    class AutodatabaseTest02ChildDefault {
        private Long id;
        private String name;
    }

    @Data
    protected class AutodatabaseTest02ChildProtected {
        private Long id;
        private String name;
    }

    @Data
    private class AutodatabaseTest02ChildPrivate {
        private Long id;
        private String name;
    }
}

@Data
class AutodatabaseTest02ChildBor {
    private Long id;
    private String name;
}