import { createNavigationContainerRef } from "@react-navigation/native";

import type { MainTabParamList } from "@/navigation/MainTabs";

export const navigationRef = createNavigationContainerRef<MainTabParamList>();
