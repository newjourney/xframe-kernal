package dev.xframe.module;

import java.lang.reflect.Field;

import dev.xframe.inject.Inject;
import dev.xframe.inject.Injection;
import dev.xframe.inject.Injection.BeanContainer;
import dev.xframe.inject.Injector;
import dev.xframe.inject.Injector.FieldInjector;

public class ModularInjection {

    public static Injector build(Class<?> clazz) {
        return Injection.build(clazz, ModularInjection::build);
    }
    public static Object inject(Object bean, Injector injector, ModuleContainer mc) {
        return Injection.inject(bean, injector, beanContainer(mc));
    }

    static BeanContainer beanContainer(ModuleContainer mc) {
        return new ModularBeanContainer(mc);
    }

    static FieldInjector build(Field field) {
        if (field.isAnnotationPresent(Inject.class) && ModularHelper.isModularClass(field.getType())) {
            return new ModularInjector(field);
        }
        return Injector.build(field);
    }
    
    static final ModuleTypeLoader mcLoader = new ModuleTypeLoader() {
        @SuppressWarnings("unchecked")
        public <T> T load(ModuleContainer container) {
            return (T) container;
        }
    };
    
    static class ModularInjector extends FieldInjector {
        final ModuleTypeLoader loader;
        public ModularInjector(Field field) {
            super(field);
            this.checkLegal();
            this.loader = getLoader(field.getType());
        }
        ModuleTypeLoader getLoader(Class<?> c) {
            return ModuleContainer.class.isAssignableFrom(c) ? mcLoader  : ModularConext.getLoader(c);
        }
        void checkLegal() {
            if(ModuleContainer.class.isAssignableFrom(type) ||
                    type.isAnnotationPresent(ModularComponent.class) ||
                    type.isInterface() ||
                    declaringIsComponentAndPackageMatch()
                    ) {
                return;
            }
            throw new IllegalArgumentException("Modular inject can`t be module impelements");
        }
        boolean declaringIsComponentAndPackageMatch() {
            return field.getDeclaringClass().isAnnotationPresent(ModularComponent.class) && field.getDeclaringClass().getPackage().getName().startsWith(type.getPackage().getName());
        }
        @Override
        protected boolean nullable(Field field) {
            return ModuleContainer.class.isAssignableFrom(field.getDeclaringClass()) ? true : super.nullable(field);
        }
        @Override
        protected Object get(BeanContainer bc) {//none cache
            Object obj = fetch(bc);
            return obj == null ? lazing(bc) : obj;
        }
        protected Object fetch(BeanContainer bc) {
            return ((ModularBeanContainer) bc).getModule(loader);
        }
    }
    
    static class ModularBeanContainer extends BeanContainer {
        private final ModuleContainer mc;
        public ModularBeanContainer(final ModuleContainer mc) {
            this.mc = mc;
        }
        public Object getModule(ModuleTypeLoader loader) {
            return loader.load(mc);
        }
    }

}
