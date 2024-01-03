import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import liveplugin.PluginUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope

for (final def project in ProjectManager.getInstance().getOpenProjects()) {
    // register an inspection
    PluginUtil.registerInspection(project, new LocalInspectionTool() {
        @Override
        String getShortName() {
            return "CanBeSpawnedInspection"
        }

        @Override
        PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
            return new PsiElementVisitor() {
                @Override
                void visitElement(@NotNull PsiElement element) {
                    super.visitElement(element)
                    if (!(element instanceof PsiClass)) {
                        return
                    }

                    PsiClass psiClass = (PsiClass) element

                    if (hasClassAnnotation(psiClass, "dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned")
                    && !implementsInterface(psiClass, "dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable")) {
                        holder.registerProblem(element, "Class annotated with @CanBeSpawned must implement Spawnable interface")
                    }
                }

                private boolean hasClassAnnotation(PsiClass psiClass, String annotation) {
                    return psiClass.annotations.any { it.hasQualifiedName(annotation) }
                }

                private boolean implementsInterface(PsiClass psiClass, String interfaceName) {
                    def findClass = JavaPsiFacade.getInstance(psiClass.project).findClass(interfaceName, GlobalSearchScope.allScope(psiClass.project))
                    return findClass != null && psiClass.isInheritor(findClass, true)
                }
            }
        }
    })

}

