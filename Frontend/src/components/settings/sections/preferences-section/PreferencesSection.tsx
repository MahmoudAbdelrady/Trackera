import { Button, InputNumber, Select, Spin } from "antd";
import { UserPreference } from "../../../";
import classes from "./scss/preferences-section.module.css";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import requestInstance from "../../../../shared/axios/request-instance";
import { useEffect, useState } from "react";
import type { JiraSite } from "../../../../shared/types";
import { isEqual } from "lodash";

const PreferencesSection = () => {
  // preferences keys
  const PREFERENCE_KEYS = {
    JIRA_PRIMARY_PROJECT: "jiraPrimaryProject",
    WORKLOGS_MONTHLY_TARGET_HOURS: "worklogsMonthlyTargetHours",
  };

  // user preferences
  const [isLoadingPreferences, setIsLoadingPreferences] = useState<boolean>(false);
  const [initialPreferences, setInitialPreferences] = useState<Record<string, any>>({});
  const [updatedPreferences, setUpdatedPreferences] = useState<Record<string, any>>({});

  // jira sites
  const [isFetchingSites, setIsFetchingSites] = useState<boolean>(false);
  const [jiraSites, setJiraSites] = useState<JiraSite[]>([]);

  useEffect(() => {
    setIsLoadingPreferences(true);
    const fetchUserPreferences = async () => {
      try {
        const response = await requestInstance.get("/user/preferences");
        const preferences: Record<string, any> = response.data;

        setInitialPreferences(preferences);
        setUpdatedPreferences(preferences);
        setJiraSites([preferences[PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT] || []]);
      } catch (error) {
        showErrorToast(error);
      }
      setIsLoadingPreferences(false);
    };

    fetchUserPreferences();
  }, []);

  const fetchJiraSites = async () => {
    setIsFetchingSites(true);
    try {
      const response = await requestInstance.get("/jira/sites");
      setJiraSites(response.data);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsFetchingSites(false);
  };

  const handlePreferenceChange = (key: string, value: any) => {
    setUpdatedPreferences((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const updateUserPreferences = async () => {
    setIsLoadingPreferences(true);
    try {
      const preferencesToUpdate = Object.fromEntries(Object.entries(updatedPreferences).filter(([key, value]) => !isEqual(initialPreferences[key], value)));
      const response = await requestInstance.post("/user/preferences", preferencesToUpdate);
      setInitialPreferences(updatedPreferences);
      showSuccessToast(response.data);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoadingPreferences(false);
  };

  return (
    <div className={classes.preferences_section}>
      {isLoadingPreferences ? (
        <Spin className={classes.loading_spinner} />
      ) : (
        <>
          <UserPreference label="Jira Main Site">
            <Select
              options={jiraSites.map((site) => ({ label: site.name, value: site.id }))}
              value={updatedPreferences[PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT]?.id}
              className={classes.preference_select}
              loading={isFetchingSites}
              notFoundContent={isFetchingSites ? <Spin size="small" /> : "No Data"}
              onOpenChange={(open) => {
                if (open) {
                  fetchJiraSites();
                }
              }}
              onChange={(value) => handlePreferenceChange(PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT, value)}
            />
          </UserPreference>
          <UserPreference label="Worklog Monthly Target Hours">
            <InputNumber
              value={updatedPreferences[PREFERENCE_KEYS.WORKLOGS_MONTHLY_TARGET_HOURS]}
              onChange={(value) => handlePreferenceChange(PREFERENCE_KEYS.WORKLOGS_MONTHLY_TARGET_HOURS, value)}
              className={classes.preference_input}
            />
          </UserPreference>
          <Button type="primary" onClick={updateUserPreferences} loading={isLoadingPreferences} disabled={isEqual(initialPreferences, updatedPreferences)} className={classes.update_button}>
            Update Preferences
          </Button>
        </>
      )}
    </div>
  );
};

export default PreferencesSection;
