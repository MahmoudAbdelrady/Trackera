import { Alert, Button, InputNumber, Select, Spin } from "antd";
import { UserPreference } from "../../../";
import classes from "./scss/preferences-section.module.css";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import { useEffect, useState } from "react";
import type { JiraSite, PreferencesProps } from "../../../../shared/types";
import { isEqual } from "lodash";
import { jiraApis, userApis } from "../../../../state/api";

const PreferencesSection = (props: PreferencesProps) => {
  const { jiraLinked, fetchPreferences, setFetchPreferences } = props;

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
    const fetchUserPreferences = async () => {
      setIsLoadingPreferences(true);
      try {
        const preferences: Record<string, any> = await userApis.getPreferences();

        setInitialPreferences(preferences);
        setUpdatedPreferences(preferences);
        setJiraSites([preferences[PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT] || []]);
      } catch (error) {
        showErrorToast(error);
      }
      setIsLoadingPreferences(false);
    };

    if (fetchPreferences) {
      fetchUserPreferences();
      setFetchPreferences(false);
    }
  }, [fetchPreferences]);

  const fetchJiraSites = async () => {
    setIsFetchingSites(true);
    try {
      const result = await jiraApis.getJiraSites();
      setJiraSites(result);
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

  const transformPreferenceValue = (key: string, value: any): any => {
    if (key === PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT) {
      return (value as JiraSite).id;
    }
    return value;
  };

  const updateUserPreferences = async () => {
    setIsLoadingPreferences(true);
    try {
      const preferencesToUpdate = Object.fromEntries(
        Object.entries(updatedPreferences)
          .filter(([key, value]) => !isEqual(initialPreferences[key], value))
          .map(([key, value]) => [key, transformPreferenceValue(key, value)])
      );
      const result = await userApis.updatePreferences(preferencesToUpdate);
      setInitialPreferences(updatedPreferences);
      showSuccessToast(result);
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
            {jiraLinked ? (
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
                onChange={(value) =>
                  handlePreferenceChange(
                    PREFERENCE_KEYS.JIRA_PRIMARY_PROJECT,
                    jiraSites.find((site) => site.id === value)
                  )
                }
              />
            ) : (
              <Alert message="Link your Jira account to enable this option." type="info" showIcon />
            )}
          </UserPreference>
          <UserPreference label="Worklog Monthly Target Hours">
            <InputNumber
              value={updatedPreferences[PREFERENCE_KEYS.WORKLOGS_MONTHLY_TARGET_HOURS]}
              onChange={(value) => handlePreferenceChange(PREFERENCE_KEYS.WORKLOGS_MONTHLY_TARGET_HOURS, value)}
              className={classes.preference_input}
            />
          </UserPreference>
          <Button
            type="primary"
            onClick={updateUserPreferences}
            loading={isLoadingPreferences}
            disabled={isEqual(initialPreferences, updatedPreferences)}
            className={classes.update_button}
          >
            Update Preferences
          </Button>
        </>
      )}
    </div>
  );
};

export default PreferencesSection;
